# Billing — Local Testing Guide

How to run the **central billing service** (`tnra-billing-app`) and a **group app**
(`tnra-app`) together on your machine and exercise the full Lemon Squeezy paywall:
checkout → webhook → entitlement → app access.

> Production setup lives in `PRODUCTION.vps.md` and `PRODUCTION.cloudflare.md`
> (section **Central Billing Service**). This file is local dev only.

## How the pieces fit

```
  member ── browser ──► tnra-app (:8080)  ── EntitlementGate ──┐
                                                               │  Bearer <per-group token>
                                                               ▼
                                          tnra-billing-app (:8082)  ── REST/JSON ──► Lemon Squeezy
                                               │  group_billing / billing_account
                                               ▼
                                          tnra_billing DB (mysql :3307)
                                               ▲
                            Lemon Squeezy ── webhook (HMAC) ──► /api/billing/webhook
```

- `tnra-app` never sees a card. It asks the billing service "is this email entitled?"
  and, on the membership screen, asks it for a **hosted Lemon Squeezy checkout URL**.
- The billing service is the **single source of truth** for entitlement. It is one
  central instance shared by all groups; each group authenticates with its own
  per-group bearer token, which fixes its `group_slug` server-side.
- Payment confirmation arrives **out of band** via the Lemon Squeezy webhook. Until the
  webhook flips the account to `ACTIVE`, the member sits at `PENDING_PAYMENT`. **This is
  why you need a public tunnel to the webhook (step 2.5) to see the full flow locally.**

Key behaviors to keep in mind while testing:

- **Entitlement fails OPEN.** If `tnra-app` can't reach the billing service, every member
  is let in (a central outage must never lock a paying group out). So if the paywall
  "isn't working," confirm the billing app is actually up and reachable.
- **Entitlement is cached 60s** per email in `tnra-app` (`BillingClientImpl.CACHE_TTL_MILLIS`).
  After a successful payment, allow up to a minute (or restart `tnra-app`) before the gate
  clears.
- **Most-permissive-wins.** A member is entitled if ANY of: group exempt, member exempt,
  group trial active (`comp_until > now`), member comp active, or subscription
  ACTIVE/grace. **The default 60-day group trial means everyone is entitled and you will
  never see the paywall** — see step 3.3 for how to register a group with the trial
  disabled so the checkout actually shows.

> **Order matters.** The billing service can't even start until its database exists, so
> set it up first (Section 1). Lemon Squeezy (Section 2) and `tnra-app` (Section 3) come
> after, then the end-to-end test (Section 4).

---

## 1. Configure `tnra-billing-app` (the central service, :8082)

Do this first — the service won't boot without its database, and `tnra-app` (Section 3)
needs both the running service and the per-group token it mints.

### 1.1 Create the `tnra_billing` database (do this first — first boot fails without it)

The billing service owns its **own** schema `tnra_billing` on the same dev MySQL the
`tnra-app` and `tnra-landing-app` use for local testing — the Docker container started by
the root `docker-compose.yml` (container name `mysql`, host port `3307`). That container
only auto-creates the `tnra` database, so `tnra_billing` does **not** exist yet. The
service uses Flyway with `ddl-auto: validate`, so on first boot it tries to connect to a
database that isn't there and fails with an `Unknown database 'tnra_billing'` error.

Create it once. Make sure the dev MySQL container is up first:

```bash
# from the repo root — start (or confirm) the shared dev infra incl. MySQL
docker compose up -d mysql

# confirm it's running and healthy (you want to see container "mysql")
docker compose ps mysql
```

Then create the database. The dev root password lives in the repo `.env`
(`MYSQL_ROOT_PASSWORD`); this reads it from there so you don't have to type it:

```bash
docker compose exec -T mysql \
  mysql -uroot -p"$(grep -E '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2-)" \
  -e "CREATE DATABASE IF NOT EXISTS tnra_billing;
      GRANT ALL PRIVILEGES ON tnra_billing.* TO 'tnra'@'%';
      FLUSH PRIVILEGES;"
```

> The `GRANT` lets the `tnra` user (the credentials the billing `application.yml` connects
> with) read/write the new schema. If your `.env` password has shell-special characters and
> the one-liner misbehaves, run `docker compose exec -it mysql mysql -uroot -p` and paste
> the three SQL statements interactively instead.

Verify it exists:

```bash
docker compose exec -T mysql \
  mysql -uroot -p"$(grep -E '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2-)" \
  -e "SHOW DATABASES LIKE 'tnra_billing';"
# → one row: tnra_billing
```

On the **next** boot of `tnra-billing-app`, Flyway runs `V1__billing_schema.sql` against
this empty database and creates `group_billing`, `billing_account`, and
`billing_webhook_event`. You only do this once; the tables persist in the `mysql-db`
Docker volume across restarts.

> Alternative (skip the manual `CREATE DATABASE`): append `&createDatabaseIfNotExist=true`
> to the datasource URL in `tnra-billing-app/src/main/resources/application.yml` so the
> JDBC driver creates the schema on connect. The explicit step above is preferred because
> it also sets up the `tnra` user grant and matches how the other services are seeded.

### 1.2 Settings overview

Create the local config from the sample (it is gitignored):

```bash
cp tnra-billing-app/src/main/resources/application.yml.sample \
   tnra-billing-app/src/main/resources/application.yml
```

The sample already points the datasource at `localhost:3307/tnra_billing`, sets
`server.port: 8082`, and pulls every secret from env (blank by default so a
misconfigured deploy fails loudly):

| Property                     | Env var             | Local value                          | Notes |
|------------------------------|---------------------|--------------------------------------|-------|
| `lemonsqueezy.api-key`       | `LS_API_KEY`        | test-mode API key                    | required for checkout (Section 2) |
| `lemonsqueezy.store-id`      | `LS_STORE_ID`       | test store id                        | required for checkout (Section 2) |
| `lemonsqueezy.webhook-secret`| `LS_WEBHOOK_SECRET` | the signing secret from step 2.4     | required for webhooks |
| `lemonsqueezy.variant.monthly` | `LS_VARIANT_MONTHLY` | monthly variant id                | required for checkout (Section 2) |
| `lemonsqueezy.variant.yearly`  | `LS_VARIANT_YEARLY`  | yearly variant id                 | required for checkout (Section 2) |
| `billing.admin-token`        | `BILLING_ADMIN_TOKEN` | any strong string                  | guards `/api/admin/**`; **blank ⇒ admin API disabled (fail closed)** |
| `billing.trial-days`         | `TNRA_TRIAL_DAYS`   | `60` (default)                       | default trial applied at registration |

### 1.3 Run it

The app **boots fine with the `LS_*` values still blank** — they're only exercised once you
start a checkout, so you can bring the service up now (which also confirms the schema fix
and the database from step 1.1), then come back and fill in the real Lemon Squeezy values
from Section 2 and restart before the end-to-end test.

Generate the admin token now (you'll reuse it when registering the group in step 3.3), and
export whatever `LS_*` values you already have (blank is OK for a first boot):

```bash
export BILLING_ADMIN_TOKEN=$(openssl rand -hex 24)   # remember this for step 3.3

# Fill these from Section 2 before running an actual checkout (blank is fine to just boot):
export LS_API_KEY=...           # test mode
export LS_STORE_ID=...
export LS_WEBHOOK_SECRET=...    # same string you set in the LS webhook (step 2.4)
export LS_VARIANT_MONTHLY=...
export LS_VARIANT_YEARLY=...

./mvnw -pl tnra-billing-app spring-boot:run
```

Sanity check:

```bash
curl http://localhost:8082/actuator/health      # {"status":"UP"}
```

> Set the **same** `BILLING_ADMIN_TOKEN` in the shell you use for the `curl` admin calls
> in step 3.3. If you run the app from the IDE, set these as run-config env vars instead.

---

## 2. Set up Lemon Squeezy (test mode)

Lemon Squeezy is the Merchant of Record. You need a (free) account; no real money moves
in **Test mode**. Collect the five `LS_*` values here, then put them into the billing
service's environment (step 1.3) and restart it.

### 2.1 Account, store, test mode

1. Sign up at <https://lemonsqueezy.com> and create a **Store**.
2. Toggle **Test mode** ON (top of the dashboard). Everything below is done in test mode.
   Test mode has its **own** API keys, store id, variant ids, and webhook secret — keep
   them separate from live values.

### 2.2 Product + two variants

Create one subscription **Product** (e.g. "TNRA Membership") with two **variants**:

| Variant | Billing  | Price | Maps to env       |
|---------|----------|-------|-------------------|
| monthly | Monthly  | $7    | `LS_VARIANT_MONTHLY` |
| yearly  | Yearly   | $60   | `LS_VARIANT_YEARLY`  |

Both must be **subscription** variants (not one-time), because the app drives the
subscription lifecycle (active / grace / cancelled) off subscription webhooks.

### 2.3 Collect the four IDs / secrets

| Value                | Where to find it                                                                 | Env var              |
|----------------------|----------------------------------------------------------------------------------|----------------------|
| **API key**          | Settings → API → create an API key                                               | `LS_API_KEY`         |
| **Store ID**         | Settings → Stores (numeric id), or the `store_id` in any API response            | `LS_STORE_ID`        |
| **Monthly variant ID** | Product → Monthly variant → the numeric id in the URL / variant detail         | `LS_VARIANT_MONTHLY` |
| **Yearly variant ID**  | Product → Yearly variant → the numeric id in the URL / variant detail          | `LS_VARIANT_YEARLY`  |

> Tip: variant ids are also returned from `GET https://api.lemonsqueezy.com/v1/variants`
> with your API key (`Authorization: Bearer <LS_API_KEY>`, `Accept: application/vnd.api+json`).

### 2.4 Webhook

1. Settings → **Webhooks** → **Add endpoint**.
2. **Signing secret**: pick any strong string → this is `LS_WEBHOOK_SECRET`.
   The service verifies `HMAC-SHA256(rawBody, secret) == X-Signature` and rejects
   anything else with 401, so this must match exactly.
3. **Events** — subscribe to at least:
   - `subscription_created`
   - `subscription_updated`
   - `subscription_payment_success`
   - `subscription_payment_failed`
   - `subscription_cancelled`
   - `subscription_expired`
4. **URL** — see step 2.5 (must be publicly reachable; `localhost` won't work).

The checkout the app creates attaches `custom_data` (`group_slug`, `beneficiary_email`,
`payer_email`); Lemon Squeezy echoes it back in `meta.custom_data` on every webhook, which
is how the service routes a payment to the right account even before a subscription id exists.

### 2.5 Expose the local webhook with a tunnel

Lemon Squeezy must reach `tnra-billing-app` on your laptop. Run a quick tunnel to
`localhost:8082`:

```bash
# Option A — cloudflared quick tunnel (no account needed)
cloudflared tunnel --url http://localhost:8082

# Option B — ngrok
ngrok http 8082
```

Take the public URL it prints and set the webhook **URL** to:

```
https://<your-tunnel-host>/api/billing/webhook
```

> Without the tunnel you can still load the checkout page and pay, but the account stays
> `PENDING_PAYMENT` forever because the confirmation webhook never arrives — entitlement
> will not flip to ACTIVE. (Advanced alternative: replay a captured webhook body locally
> with a hand-computed `X-Signature` HMAC; the tunnel is far easier.)

### 2.6 Test card

In the hosted checkout (test mode) use Stripe's test card:

```
4242 4242 4242 4242   any future expiry   any CVC   any ZIP
```

> With these values collected, set the `LS_*` env vars in the billing service (step 1.3)
> and **restart it** so checkout and webhook verification work.

---

## 3. Configure `tnra-app` (the group app, :8080)

`tnra-app` runs locally exactly as it does today (Keycloak + MySQL via the dev
`docker-compose.yml`, the app itself from your IDE / `./mvnw`). Billing adds three
settings and they are **off by default**.

### 3.1 Settings overview

`tnra-app` reads these (`application.yml.sample` documents them; the live
`application.yml` does **not** include the block yet — add it, or pass the env vars):

| Property               | Env var                 | Local value                  | Meaning                                   |
|------------------------|-------------------------|------------------------------|-------------------------------------------|
| `tnra.billing.enabled` | `TNRA_BILLING_ENABLED`  | `true`                       | Turns on the entitlement gate + `/billing` view. Off ⇒ no `BillingClient` bean, app behaves as open-source self-host. |
| `tnra.billing.api-url` | `TNRA_BILLING_API_URL`  | `http://localhost:8082`      | Where the central billing service lives.  |
| `tnra.billing.api-token` | `TNRA_BILLING_API_TOKEN` | _(from step 3.3)_          | The group's per-group bearer token.       |

### 3.2 Add the billing block to `tnra-app/src/main/resources/application.yml`

Under the existing top-level `tnra:` key, add:

```yaml
tnra:
  # ...existing auth / app / encryption / notify keys stay as they are...
  billing:
    enabled: ${TNRA_BILLING_ENABLED:false}
    api-url: ${TNRA_BILLING_API_URL:http://localhost:8082}
    api-token: ${TNRA_BILLING_API_TOKEN:}
```

Then run the app with the env set (IDE run config or shell):

```bash
export TNRA_BILLING_ENABLED=true
export TNRA_BILLING_API_URL=http://localhost:8082
export TNRA_BILLING_API_TOKEN=<paste the token from step 3.3>

# Where Lemon Squeezy returns the buyer after a successful checkout (tnra.app.base-url).
# Locally this defaults to the dev URL, so set it to your local app to be returned here:
export APP_BASE_URL=http://localhost:8080
```

> `api-token` is required when `enabled=true`. The `BillingClientImpl` bean only exists
> when `tnra.billing.enabled=true`, and `@Value("${tnra.billing.api-token}")` will fail
> fast on startup if it is missing — so register the group (step 3.3) **before** you
> start `tnra-app` with billing on.
>
> The app passes its base URL to the checkout as `product_options.redirect_url`, so after
> paying the member is returned to `tnra-app` (no Lemon Squeezy dashboard config needed).

### 3.3 Make the test group ready to test billing

Locally, `tnra-app` authenticates against the OG `tnra` realm. The **group identity used
for billing is whatever slug you register centrally** — `tnra-app` never sends it; it is
derived from the bearer token by `GroupTokenAuthFilter`. So registration both creates the
group's billing record *and* mints the token you paste into `TNRA_BILLING_API_TOKEN`.

Register the group **with the trial disabled** so the paywall is actually exercised
(`trialDays: 0`, `exempt: false` — otherwise the 60-day `GROUP_TRIAL` entitles everyone
and you'll never see checkout). This calls the admin API on the running billing app
(brought up in step 1.3), authenticated by `X-Admin-Token` = your `BILLING_ADMIN_TOKEN`:

```bash
curl -s -X POST http://localhost:8082/api/admin/groups \
  -H "X-Admin-Token: $BILLING_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"groupSlug":"local","trialDays":0,"exempt":false}'
```

Response (the token is shown **once** — copy it now):

```json
{ "groupSlug": "local", "apiToken": "<COPY THIS into TNRA_BILLING_API_TOKEN>", "trialEndsAt": null }
```

Useful variations while testing the entitlement matrix:

```bash
# Whole-group trial (everyone entitled via GROUP_TRIAL for 14 days)
curl -X PATCH http://localhost:8082/api/admin/groups/local \
  -H "X-Admin-Token: $BILLING_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"trialDays":14}'

# Forever-free pilot group (everyone entitled via GROUP_EXEMPT)
curl -X PATCH http://localhost:8082/api/admin/groups/local \
  -H "X-Admin-Token: $BILLING_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"exempt":true}'

# Back to "must pay" (trial off, not exempt)
curl -X PATCH http://localhost:8082/api/admin/groups/local \
  -H "X-Admin-Token: $BILLING_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"exempt":false,"trialDays":0}'
```

---

## 4. End-to-end smoke test

Start order: **MySQL + Keycloak (docker)** → **tnra-billing-app (:8082)** with the real
`LS_*` values (steps 1.3 + Section 2) → register the group (step 3.3) → **tnra-app (:8080)**
with the token → **tunnel (step 2.5)**.

1. Log into `tnra-app` as a member (e.g. `http://localhost:8080`, OG `tnra` realm user).
2. Because the group has no trial/exempt and the member has no subscription, the
   `EntitlementGate` forwards any protected route to **`/billing`** ("Your membership").
3. Click **Pay $7 / month** → you're sent to the hosted Lemon Squeezy checkout. Pay with
   the test card (step 2.6).
4. On success, Lemon Squeezy returns you to **`/billing/activating`** ("Activating your
   membership…"). Meanwhile it fires `subscription_created` / `subscription_payment_success`
   to your tunnel → `/api/billing/webhook` → the account flips to `ACTIVE`.
5. The activating page polls entitlement (cache-bypassing) every ~2s and forwards you into
   the app the moment the webhook lands — usually a few seconds, no manual refresh. If the
   webhook is slow (>~60s) it shows a **Continue to TNRA** button instead.
6. **Duplicate-charge guard:** once you're `ACTIVE`, going back to `/billing` and clicking a
   pay button again returns **409** and shows "You're already a member…" instead of starting
   a second subscription. (Gifting to another member, and re-subscribing after `SUSPENDED`,
   are still allowed.)
7. Verify the data:

```bash
# entitlement straight from the API (use the per-group token, not the admin token)
curl -s "http://localhost:8082/api/v1/entitlement?email=member@example.com" \
  -H "Authorization: Bearer $TNRA_BILLING_API_TOKEN"
# {"entitled":true,"status":"ACTIVE","reason":"SUBSCRIPTION"}

# inspect rows
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" tnra_billing \
  -e "SELECT email,status,payer_email,ls_subscription_id FROM billing_account;
      SELECT event_name,processed,error FROM billing_webhook_event ORDER BY id DESC LIMIT 5;"
```

8. **Manage subscriptions.** "Update payment method" on `/billing` opens the hosted Customer
   Portal (`/api/v1/portal` → LS `customer_portal` URL), which lists **every** subscription you
   pay for — your own *and* any gifts — so you can update a card or **cancel a specific gift**
   there. If you're covering anyone, `/billing` also shows a **"Members you're covering"** list
   (status per beneficiary). The portal link works even for a **pure gifter** (a trial/comp member
   with no subscription of their own) — it falls back to a subscription they pay for.

> If after paying the member is still sent to the membership page, the webhook didn't
> arrive — check `SELECT status FROM billing_account` (still `PENDING_PAYMENT`) and
> `SELECT * FROM billing_webhook_event` (empty). The redirect back to the app and the
> webhook are independent; only the webhook flips entitlement. Either fix delivery
> (tunnel + LS webhook URL, step 2.5) or simulate it locally (below).

### Simulate the webhook locally (no tunnel)

If you don't want to run a public tunnel, POST a synthetic `subscription_created` event
straight to the billing app. The account row already exists (`PENDING_PAYMENT`) from the
checkout, so this flips it to `ACTIVE` exactly as a real Lemon Squeezy webhook would.

The body must be signed with HMAC-SHA256 using your `LS_WEBHOOK_SECRET` (the service
verifies it and returns 401 otherwise). Run this in the **same shell where
`LS_WEBHOOK_SECRET` is exported**, and set the `custom_data` values to match the account
you created (group slug + beneficiary email):

```bash
BODY='{"meta":{"event_name":"subscription_created","custom_data":{"group_slug":"local","beneficiary_email":"member@example.com","payer_email":"member@example.com"}},"data":{"id":"sim-sub-1","attributes":{"customer_id":"sim-cust-1","status":"active"}}}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$LS_WEBHOOK_SECRET" | awk '{print $NF}')
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8082/api/billing/webhook \
  -H "Content-Type: application/json" -H "X-Signature: $SIG" --data-binary "$BODY"
# → 200   (401 = LS_WEBHOOK_SECRET mismatch)
```

Re-check entitlement (`status` should now be `ACTIVE`) and reload `tnra-app` (allow the 60s
cache or restart). Vary the event to exercise the rest of the matrix: `data.attributes.status`
`past_due` → `ON_GRACE_PERIOD`, `cancelled`/`expired` → `SUSPENDED`; or set
`meta.event_name` to `subscription_payment_failed` (→ grace) or `subscription_cancelled`
(→ suspended).

### Things to try

- **Profile → Billing tab**: everything in one place — membership status (**pay options shown only
  when you have no active subscription**, **"Update payment method"** always shown), a **"Gift a
  membership"** section, and a **"Members you're covering"** list (the gifts you pay for). The status
  blurb reflects your actual state: **"Your membership is active"** when you have a subscription,
  **"…gifted by <name>"** when someone else pays for you, or the **"Activate your membership…"** prompt
  when you have none. The tab's billing-service calls are **lazy** — they only fire when you actually
  open the Billing tab, not on every profile visit.
- **Gift a membership (UI)**: as an **active** member, open **Profile → Billing → Gift a
  membership**, pick another member, and click **Continue to checkout** → you land on `/billing` in
  gift mode ("Gift a membership"). Choosing a plan sends you to Lemon Squeezy with the **beneficiary's**
  email; you (the payer) are charged, and the webhook activates *their* account. The gift section
  is greyed out (with a notice) for members who aren't active themselves. The payer is always
  your authenticated identity — only the beneficiary comes from the selection, and it's
  validated to be a real group member. If the beneficiary is **already settled** (active sub,
  trial, exempt, or an existing gift) the gift is blocked — the UI shows "already has an active
  membership — no gift needed" and the billing API returns **409** (enforced even if the UI is
  bypassed). A beneficiary in **`ON_GRACE_PERIOD`** (dunning — their own payment is failing) is
  intentionally still **giftable**: the gift is a rescue that supersedes the failing subscription.
- **Gift / covering (API)**: a gift is `payerEmail` ≠ `beneficiaryEmail`.
  `GET /api/v1/covering?payerEmail=...` lists who a payer covers. A `billing_webhook_event`
  with a non-null `error` and `processed=false` is an **unmatched** event kept for
  reconciliation — check the `custom_data` routing.
- **Dunning**: send a `subscription_payment_failed` (from the LS dashboard's webhook
  "send test" or by letting a renewal fail) → status `ON_GRACE_PERIOD`, still entitled.
- **Cancellation**: `subscription_cancelled`/`subscription_expired` → `SUSPENDED`, gate
  forwards to `/billing` again.

### Troubleshooting

| Symptom | Likely cause |
|---------|--------------|
| `tnra-billing-app` won't start; `Unknown database 'tnra_billing'` | Database not created — do step 1.1 before first boot. |
| Paywall never shows; member always gets in | Group is exempt or in trial (`GROUP_EXEMPT`/`GROUP_TRIAL`), or billing service is down and the gate is failing open. Re-register with `trialDays:0, exempt:false`; confirm `:8082` is UP. |
| `tnra-app` won't start with billing on | `TNRA_BILLING_API_TOKEN` missing/blank while `enabled=true`. Register the group first, paste the token. |
| Checkout 500 / "couldn't start checkout" | Bad `LS_API_KEY` / `LS_STORE_ID` / variant id, or live-mode keys used in test mode. |
| Account stuck `PENDING_PAYMENT` after paying | Webhook never reached the service — tunnel down, wrong webhook URL, or `LS_WEBHOOK_SECRET` mismatch (401, nothing persisted). |
| Lemon Squeezy shows the webhook failing with **401/403** | Signature rejected. The webhook's **Signing secret** in Lemon Squeezy must be the **exact same string** as the billing app's `LS_WEBHOOK_SECRET` — and it must be **non-blank** (a blank secret rejects every webhook). Set both, restart the billing app. (A 403 here is really the 401 surfacing through the `/error` dispatch.) |
| `401 Admin API not configured` on register | `BILLING_ADMIN_TOKEN` blank in the billing app, or the `X-Admin-Token` header doesn't match. |
| Entitlement slow to change | 60s client cache in `tnra-app`; wait or restart. |
```
