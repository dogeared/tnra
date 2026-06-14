# Billing / Subscription Feature — Execution Plan

Status: reviewed via /plan-eng-review on 2026-06-14 (incl. Codex outside-voice pass). Branch: `feat/billing`.

## Architecture: a dedicated central billing service

Multi-tenancy here is physical — each group is its own MySQL DB + Keycloak realm + subdomain.
Lemon Squeezy is ONE store with ONE webhook URL. A per-group app cannot answer "which group's
database does this payment event belong to?" (user ids are per-DB; there's no runtime registry).
So billing lives in a NEW central module, **`tnra-billing-app`**, that owns the LS relationship
and is the single source of truth. The per-group `tnra-app` holds NO billing state; it asks the
billing app whether a member is entitled. The landing app is untouched.

```
                         ┌─────────────────────────────────────────────┐
   Lemon Squeezy ─webhook(1 URL)─► tnra-billing-app (central, own DB)    │
   (single store)        │   billing_account {group_slug,email,ls_ids,   │
        ▲  checkout/      │                    status,exempt,comp_until}  │
        │  portal API     │   group_billing {group_slug,exempt,comp}     │
        │                 │   billing_webhook_event {raw payload, replay} │
        │                 └───▲───────────────▲──────────────────────────┘
   create checkout / portal   │ entitlement?  │ create-checkout / portal
        │                     │ (cached,      │ (per-group bearer token →
   ┌────┴─────┐          ┌────┴─────┐         │  central derives group_slug)
   │ group A  │          │ group B  │  ...    │
   │ tnra-app │          │ tnra-app │   each calls central; holds no billing truth
   └──────────┘          └──────────┘
```

### Why this resolves the Codex findings

- **#1 webhook→tenant routing / #6 missing central router:** central app IS the router. An LS
  event carries `ls_subscription_id`/`ls_customer_id`, which maps to a `billing_account` row that
  already records `{group_slug, email}` (written at checkout creation). No ambiguity, no per-group
  fan-out.
- **#2 lost payment:** `billing_webhook_event` stores the RAW payload + status + error. Unmatched
  events are persisted for reconciliation/replay, never dropped as a no-op.
- **#3 redirect/webhook race + duplicate checkout:** central records a `billing_account` row
  (status PENDING_PAYMENT) BEFORE returning the checkout URL, and dedupes on `{group_slug, email}` —
  an existing PENDING/ACTIVE row is reused, not duplicated. The gate reads status from central, so a
  redirect-back-before-webhook shows "payment processing" until the webhook flips status.
- **#4 unseeded `group_settings`:** group-level exempt/comp live in central `group_billing`, not the
  group DB — the seeded-row problem disappears, and the group DB needs NO billing migration.
- **#5 webhook endpoint plumbing:** `tnra-billing-app` is a fresh Spring app; its SecurityConfig
  permits anonymous `POST /api/billing/webhook` with CSRF disabled for that path from day one.
  Group↔central API calls use a per-group bearer token.

## The entitlement gate (now a central computation)

`tnra-billing-app` exposes `GET /api/v1/entitlement` (authenticated by the caller's per-group
token, which fixes `group_slug`). It computes, most-permissive-wins:

```
entitled(group_slug, email):
  1. group_billing.exempt                          -> true   # pilot group, forever free
  2. account.exempt                                -> true   # individual permanent comp
  3. group_billing.comp_until > now                -> true   # whole-group promo
  4. account.comp_until > now                       -> true   # individual time-boxed waiver
  5. account.status in {ACTIVE, ON_GRACE_PERIOD}    -> true   # actually paying
  else                                              -> false  # PENDING_PAYMENT or SUSPENDED
  (no account row yet & billing enabled            -> false / PENDING_PAYMENT)
```

The per-group app gates with this; if `tnra.billing.enabled=false` it never calls central and
treats everyone as entitled (self-host).

## tnra-billing-app (new module)

**Data model (central DB):**

```sql
CREATE TABLE group_billing (
  group_slug   VARCHAR(64) PRIMARY KEY,
  exempt       BOOLEAN  NOT NULL DEFAULT FALSE,
  comp_until   DATETIME NULL,
  api_token_hash VARCHAR(128) NOT NULL,        -- per-group bearer token (hashed)
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE billing_account (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_slug         VARCHAR(64)  NOT NULL,
  email              VARCHAR(255) NOT NULL,    -- BENEFICIARY (who is entitled)
  payer_email        VARCHAR(255) NULL,        -- NULL = self-pay; set = gifted by this person
  status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING_PAYMENT',
  exempt             BOOLEAN      NOT NULL DEFAULT FALSE,
  comp_until         DATETIME     NULL,
  ls_customer_id     VARCHAR(64)  NULL,
  ls_subscription_id VARCHAR(64)  NULL,
  ls_variant         VARCHAR(16)  NULL,        -- 'monthly' | 'yearly'
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_group_email (group_slug, email),
  KEY idx_subscription (ls_subscription_id),
  KEY idx_customer (ls_customer_id),
  KEY idx_payer (group_slug, payer_email)      -- "subscriptions I'm covering"
);

CREATE TABLE billing_webhook_event (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  ls_event_id  VARCHAR(128) NOT NULL UNIQUE,   -- idempotency
  event_name   VARCHAR(64)  NOT NULL,
  raw_payload  MEDIUMTEXT   NOT NULL,          -- full body for replay/reconciliation
  matched_account_id BIGINT NULL,              -- null = unmatched, needs reconciliation
  processed    BOOLEAN      NOT NULL DEFAULT FALSE,
  error        TEXT         NULL,
  received_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Endpoints:**
- `POST /api/v1/checkout` (group token) — body `{beneficiary_email, variant, payer_email}`; group_slug from
  token. `payer_email == beneficiary_email` → self-pay; differ → GIFT. Reuse-or-create the BENEFICIARY's
  `billing_account` (dedup on `{group_slug, beneficiary_email}`), set `payer_email`, call LS `POST /v1/checkouts`
  with `custom` `{group_slug, beneficiary_email, payer_email}`, return hosted URL (charged to the payer).
- `GET /api/v1/entitlement?email=` (group token) — returns `{entitled, status, reason}` (beneficiary-keyed;
  gift vs self-pay is irrelevant to entitlement).
- `GET /api/v1/covering?payer_email=` (group token) — accounts this person is paying for (their gift list).
- `GET /api/v1/portal?email=` (group token) — LS Customer Portal URL for the PAYER of that account.
- `POST /api/billing/webhook` (LS only) — HMAC-SHA256 verify over raw body (401 on mismatch);
  insert `ls_event_id` (dup → 200 no-op); persist raw; resolve account via subscription/customer id;
  map event → status (`subscription_created`/`updated` → ACTIVE or ON_GRACE_PERIOD per LS status;
  `subscription_payment_failed` → ON_GRACE_PERIOD; `subscription_expired`/`cancelled` → SUSPENDED);
  unmatched → leave `matched_account_id` null + flag for reconciliation; 200 after persist.

**Config:** `LS_API_KEY`, `LS_STORE_ID`, `LS_VARIANT_MONTHLY`, `LS_VARIANT_YEARLY`, `LS_WEBHOOK_SECRET`,
DB connection. Build: new Maven module under the parent aggregator pom (version tracks `VERSION`).
Deploy: central single instance alongside landing (add to `docker-compose.production.yml`); NOT run
by self-hosters.

## tnra-app (per-group) changes — minimal, no DB migration

- `BillingClient` (Spring `RestClient`) → central API, `@ConditionalOnProperty(name="tnra.billing.enabled", havingValue="true")`.
- Entitlement gate: a Vaadin `BeforeEnterListener` calls `BillingClient.isEntitled(email)` for gated
  routes, allowing only `ProfileView` + `/billing`. **Cache ~60s per member; fail-OPEN** if central is
  unreachable (never lock out a paying member over a transient outage).
- `/billing` view: "Pay (monthly/yearly)" → central `/checkout` → redirect; "Update payment" → central
  `/portal` URL. Shows "payment processing" when status is PENDING right after redirect-back.
- Config: `TNRA_BILLING_ENABLED` (default false), `TNRA_BILLING_API_URL`, `TNRA_BILLING_API_TOKEN`.
- **Accepted tradeoffs:** (a) the ~60s entitlement cache means a status change (trial expiry, suspension)
  can take up to ~60s / next navigation to bite — fine for this app. (b) fail-open means a central-app
  outage silently disables the paywall (everyone stays entitled); chosen over locking out paying members.
- **No new columns on `users`/`group_settings`.** No V13 on the group DB. `User.active` unchanged
  (admin enable/disable stays orthogonal and local).

## Gift subscriptions (member or admin pays for another member) — SELECTIVE EXPANSION add

Payer ≠ beneficiary, but still ONE subscription per beneficiary (NOT a quantity-N group subscription —
that would resurrect the rejected per-seat model). "Admin pays for the whole group annually" = N
individual gift subscriptions under the admin's card, each entitling one member; reimbursement is the
admin's private concern (out of scope). Eases the launch friction risk: a committed member/founder can
carry anyone who'd otherwise stall at the payment wall, so money never breaks the call chain. Also
captures the church/ministry-sponsor case through the per-member model (no separate org tier needed).

- **Data:** `billing_account.payer_email` (null = self-pay; set = gifted). Entitlement is beneficiary-keyed
  and unchanged.
- **Flow:** payer P opens checkout for beneficiary M (`POST /api/v1/checkout {beneficiary_email:M, payer_email:P}`);
  LS subscription is created under P's customer/card; webhook maps it to M's `billing_account` (M becomes
  ACTIVE) with `payer_email=P`.
- **v1 scope:** single-beneficiary gifting ("cover this member") from the member list, for self OR admin,
  monthly or annual. Admin-covers-whole-group works in v1 by gifting members one at a time.
- **Gift payment-failure edge (MUST design in):** if the PAYER's card fails, the BENEFICIARY would otherwise
  be suspended through no fault of their own and can't fix it. Rule: on a gifted-subscription
  `payment_failed`/`expired`, NOTIFY the payer to update their card AND let the beneficiary SELF-PAY to take
  over. Same on gift cancellation: notify the beneficiary; they self-pay or fall back to any active trial/comp.
- **Supersede is a TWO-SIDED operation (money bug if half-done):** when a beneficiary self-pays to replace a
  gift, the prior gift subscription MUST be CANCELLED in Lemon Squeezy (API call), not merely detached in our
  DB — otherwise the gifter keeps getting charged for a subscription that no longer entitles anyone. Order:
  create the self-pay subscription → on its `subscription_created` webhook, cancel the old gift sub in LS →
  then clear `payer_email`/swap `ls_subscription_id`. Test that the old sub is actually cancelled in LS.
- **Self-pay identity guard:** for self-pay checkout, the server forces `beneficiary_email` = the authenticated
  OIDC email (ignore any client-supplied value); only gifts may name a different beneficiary. Prevents a
  member checking out under a different email and then computing as PENDING. Tests required for all the above.

## Launch posture: group free-trial default-ON

New groups start in a free-trial window so the brotherhood forms BEFORE anyone is asked to pay (mitigates
the per-member friction risk). Provisioning sets `group_billing.comp_until = now + TRIAL_DAYS`
(`TRIAL_DAYS` default 60, CLI-overridable). During the trial every member is entitled; at expiry, members
without a subscription (self-paid or gifted) flip to PENDING_PAYMENT on next login. `--billing-exempt`
still means forever-free (pilot), distinct from the time-boxed trial.

## CLI / provisioning (tnra-cli-app)

- On provision: register the group in central via an admin call (creates `group_billing` row with
  `comp_until = now + TRIAL_DAYS`, mints the per-group API token), write `TNRA_BILLING_*` into the group's
  `.env`/`docker-compose` (blank by default so self-host stays off). `--billing-exempt` / `--comp-until` /
  `--trial-days` override the central `group_billing` row, not the group DB.

## Rollout / existing live group (CRITICAL)

When enabling billing for the already-running group, seed central `billing_account` rows as `ACTIVE`
for existing members (or set `group_billing.exempt=true` until they're migrated to real subscriptions).
Without this they'd compute as PENDING_PAYMENT and be locked out. (Replaces the old per-DB backfill;
no group-DB migration now.) **Regression test required.**

## Admin edge cases

- **Admin renewal fails:** admin is a member with the ADMIN role → ON_GRACE_PERIOD → SUSPENDED.
  Per-member billing means other members are unaffected (separate subscriptions). Suspended admin sees
  profile + pays to restore; admin views gated like everything else. Group keeps functioning.
- **Last-admin-suspended:** admin tasks pause until that admin restores payment (self-serve), or comp
  the admin's account centrally. Non-blocking.

## NOT in scope (deferred)

- Bulk "cover all uncovered members" one-click gift action — fast-follow; v1 gifts member-by-member.
- Org/ministry-sponsor tier as a distinct product — gift subscriptions cover this case for now.
- Partial/percentage discounts (LS coupons, price-level) — defer.
- Proration / mid-cycle plan switch — LS Portal handles it; react to `subscription_updated`.
- Local "comp_until expiring soon" reminder email — needs a scheduled job in the billing app; deferred
  to TODO (v1 enforcement is lazy-at-gate, no cron required).
- Webhook reconciliation UI/admin for unmatched events — v1 logs + flags them; manual reconcile via DB.
- In-app refunds — via LS dashboard.
- Stripe Managed Payments migration — revisit when GA.
- Push-based propagation / central-writes-to-group-DBs — rejected in favor of pull-API + cache
  (looser coupling, smaller blast radius).

## What already exists (reuse)

- Per-group gate chokepoint pattern: `UserServiceImpl.getCurrentUser()` + Vaadin `@RolesAllowed`/`@PermitAll`.
- OIDC email identity for the member key (`OidcUserService`).
- Parent aggregator pom + existing module layout (tnra-app / tnra-cli-app / tnra-landing-app) — add
  `tnra-billing-app` as a peer.
- Encryption util (`AesGcm`, `EncryptedStringConverter`) — apply to LS ids in the central DB if desired.
- `groups.json` provisioning registry — extend provisioning to also register the group centrally.

## Test plan (every path ships with its test)

**tnra-billing-app:**
- `EntitlementService` — all 6 branches incl. both sides of each `comp_until`, each status, and
  no-account-row case.
- Webhook controller — bad signature → 401; duplicate `ls_event_id` → 200 no-op; each event type →
  status transition; unmatched subscription id → persisted + flagged (NOT dropped); raw payload stored.
- Checkout — dedup reuses existing PENDING/ACTIVE row for `{group_slug,email}`; creates row before
  returning URL.
- Auth — per-group token scopes requests to that group_slug only (cross-group read/write rejected).
- Security config — anonymous webhook path permitted + CSRF-exempt; API paths require token.

**tnra-app:**
- `BillingClient`/gate — entitled vs not redirects correctly; billing disabled → no central call,
  everyone entitled; central-unreachable → fail-open (cached/last-known); `/billing` + `ProfileView`
  reachable while suspended, posts/stats not.
- E2E: first-login → checkout → ACTIVE (monthly + yearly); renewal-fail → grace → suspended;
  suspended → portal → update → ACTIVE; redirect-back-before-webhook shows processing then resolves.

**Gift + trial:**
- Gift checkout: payer≠beneficiary records beneficiary ACTIVE with payer_email set; entitlement unchanged.
- Gift payment-failure: payer card fails → beneficiary grace/suspended → beneficiary self-pay supersedes
  (payer_email cleared); gift cancellation → beneficiary notified + can self-pay.
- `GET /covering` lists a payer's gifted accounts.
- Group trial: provision sets comp_until = now + TRIAL_DAYS; members entitled during trial; at expiry,
  uncovered members → PENDING_PAYMENT; trial (time-boxed) vs exempt (forever) behave distinctly.

**Rollout:** seeding existing members ACTIVE (or group exempt) — **CRITICAL regression**.

Test command: `./mvnw clean test` (all modules, incl. new tnra-billing-app).
```
