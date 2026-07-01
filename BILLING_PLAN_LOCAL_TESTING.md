# Billing — Local Testing Guide (Paddle)

How to run the **central billing service** (`tnra-billing-app`) and a **group app**
(`tnra-app`) together on your machine and exercise the full Paddle paywall:
checkout → webhook → entitlement → app access.

> **Provider:** this branch uses **Paddle** (Merchant of Record). Production setup lives in
> `PRODUCTION.vps.md` / `PRODUCTION.cloudflare.md` (section **Central Billing Service**).
> This file is local dev only.
>
> The DB columns are still named `ls_*` (`ls_subscription_id`, `ls_customer_id`) — they are
> opaque provider ids and hold Paddle ids on this branch; the name wasn't churned.

## How the pieces fit

```
  member ── browser ──► tnra-app (:8080)  ── EntitlementGate ──┐
                                                               │  Bearer <per-group token>
                                                               ▼
                                          tnra-billing-app (:8082)  ── REST/JSON ──► Paddle
                                               │  group_billing / billing_account
                                               ▼
                                          tnra_billing DB (mysql :3307)
                                               ▲
                                  Paddle ── webhook (Paddle-Signature) ──► /api/billing/webhook
```

- `tnra-app` never sees a card. It asks the billing service "is this email entitled?"
  and, on the membership screen, asks it for a **hosted Paddle checkout URL**.
- The billing service is the **single source of truth** for entitlement. It is one
  central instance shared by all groups; each group authenticates with its own
  per-group bearer token, which fixes its `group_slug` server-side.
- Payment confirmation arrives **out of band** via the Paddle webhook. Until the
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
> set it up first (Section 1). Paddle (Section 2) and `tnra-app` (Section 3) come
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
docker compose ps mysql
```

Then create the database (reads the dev root password from the repo `.env`):

```bash
docker compose exec -T mysql \
  mysql -uroot -p"$(grep -E '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2-)" \
  -e "CREATE DATABASE IF NOT EXISTS tnra_billing;
      GRANT ALL PRIVILEGES ON tnra_billing.* TO 'tnra'@'%';
      FLUSH PRIVILEGES;"
```

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

### 1.2 Settings overview

Create the local config from the sample (it is gitignored):

```bash
cp tnra-billing-app/src/main/resources/application.yml.sample \
   tnra-billing-app/src/main/resources/application.yml
```

The sample already points the datasource at `localhost:3307/tnra_billing`, sets
`server.port: 8082`, and pulls every secret from env (blank by default so a
misconfigured deploy fails loudly). The Paddle settings live under the `paddle:` key:

| Property                 | Env var               | Local value                                   | Notes |
|--------------------------|-----------------------|-----------------------------------------------|-------|
| `paddle.api-key`         | `PADDLE_API_KEY`      | sandbox API key                               | required for checkout (Section 2) |
| `paddle.api-base`        | `PADDLE_API_BASE`     | `https://sandbox-api.paddle.com` (default)    | leave unset for sandbox; set `https://api.paddle.com` for live |
| `paddle.webhook-secret`  | `PADDLE_WEBHOOK_SECRET` | the destination secret from step 2.4 (`pdl_ntfset_…`) | required for webhooks |
| `paddle.price.monthly`   | `PADDLE_PRICE_MONTHLY` | monthly Paddle **Price** id (`pri_…`)        | required for checkout (Section 2) |
| `paddle.price.yearly`    | `PADDLE_PRICE_YEARLY`  | yearly Paddle **Price** id (`pri_…`)         | required for checkout (Section 2) |
| `billing.admin-token`    | `BILLING_ADMIN_TOKEN` | any strong string                             | guards `/api/admin/**`; **blank ⇒ admin API disabled (fail closed)** |
| `billing.trial-days`     | `TNRA_TRIAL_DAYS`     | `60` (default)                                | default trial applied at registration |

### 1.3 Run it

The app **boots fine with the `PADDLE_*` values still blank** — they're only exercised once
you start a checkout, so you can bring the service up now (which also confirms the schema
fix and the database from step 1.1), then come back and fill in the real Paddle values from
Section 2 and restart before the end-to-end test.

Generate the admin token now (you'll reuse it when registering the group in step 3.3), and
export whatever `PADDLE_*` values you already have (blank is OK for a first boot):

```bash
export BILLING_ADMIN_TOKEN=$(openssl rand -hex 24)   # remember this for step 3.3

# Fill these from Section 2 before running an actual checkout (blank is fine to just boot):
export PADDLE_API_KEY=...            # sandbox
export PADDLE_WEBHOOK_SECRET=...     # pdl_ntfset_... from the notification destination (step 2.4)
export PADDLE_PRICE_MONTHLY=pri_...  # $7 / month
export PADDLE_PRICE_YEARLY=pri_...   # $60 / year
# PADDLE_API_BASE defaults to https://sandbox-api.paddle.com — leave unset for sandbox.

./mvnw -pl tnra-billing-app spring-boot:run
```

Sanity check:

```bash
curl http://localhost:8082/actuator/health      # {"status":"UP"}
```

> Set the **same** `BILLING_ADMIN_TOKEN` in the shell you use for the `curl` admin calls
> in step 3.3. If you run the app from the IDE, set these as run-config env vars instead.

---

## 2. Set up Paddle (sandbox)

Paddle is the Merchant of Record. You need a **sandbox** account; no real money moves.
The sandbox is a **separate login** from live and has its **own** API key, Price ids, and
webhook secret — keep them separate from live values. Collect the values here, then put
them into the billing service's environment (step 1.3) and restart it.

> Paddle sandbox: <https://sandbox-vendors.paddle.com>. The API base is
> `https://sandbox-api.paddle.com` (already the default in `application.yml.sample`).

### 2.1 Product + two prices

**Catalog → Products** → create one product (e.g. "TNRA Membership"), then add two
recurring **Prices**:

| Price   | Billing period | Amount | Maps to env           |
|---------|----------------|--------|-----------------------|
| monthly | Monthly        | $7     | `PADDLE_PRICE_MONTHLY` |
| yearly  | Yearly         | $60    | `PADDLE_PRICE_YEARLY`  |

Copy each **Price id** (`pri_…`) from the price detail. Both must be recurring
(subscription) prices — the app drives the subscription lifecycle
(active / grace / cancelled) off subscription webhooks.

### 2.2 API key

**Developer Tools → Authentication** → create a sandbox **API key** → `PADDLE_API_KEY`.
(The client sends it as `Authorization: Bearer <PADDLE_API_KEY>` against
`https://sandbox-api.paddle.com`.)

### 2.3 Default payment link (required for hosted checkout)

The app creates a checkout via `POST /transactions` and reads `data.checkout.url` back.
**Paddle only returns that URL if a default payment link is configured.** Set it under
**Checkout → Settings → Default payment link** (any URL on a domain approved for your
sandbox seller — the hosted overlay/inline checkout is served from there). Without it,
`createCheckout` gets a transaction with no `checkout.url` and the membership screen fails
to start checkout.

### 2.4 Webhook (notification destination)

**Developer Tools → Notifications → New destination**:

1. **URL** — see step 2.5 (must be publicly reachable; `localhost` won't work). It ends in
   `/api/billing/webhook`.
2. **Secret key** — Paddle generates a `pdl_ntfset_…` secret for the destination → this is
   `PADDLE_WEBHOOK_SECRET`. The service verifies the `Paddle-Signature` header
   (`ts=<unix>;h1=<hex>`, where `h1 == HMAC-SHA256("<ts>:<rawBody>", secret)`) and rejects
   anything else with 401, so this must match exactly.
3. **Events** — subscribe to at least:
   - `subscription.created`
   - `subscription.activated`
   - `subscription.updated`
   - `subscription.canceled`
   - `subscription.past_due`
   - `subscription.paused`
   - `subscription.resumed`
   - `transaction.completed`
   - `transaction.payment_failed`

The checkout the app creates attaches `custom_data` (`group_slug`, `beneficiary_email`,
`payer_email`); Paddle echoes it back as `data.custom_data` on every webhook, which is how
the service routes a payment to the right account even before a subscription id exists.
(`subscription.*` events carry the subscription id as `data.id`; `transaction.*` events
carry it as `data.subscription_id`.)

### 2.5 Expose the local webhook with a tunnel

Paddle must reach `tnra-billing-app` on your laptop. Run a quick tunnel to `localhost:8082`:

```bash
# Option A — cloudflared quick tunnel (no account needed)
cloudflared tunnel --url http://localhost:8082

# Option B — ngrok
ngrok http 8082
```

Set the destination **URL** (step 2.4) to:

```
https://<your-tunnel-host>/api/billing/webhook
```

> Without the tunnel you can still load the checkout page and pay, but the account stays
> `PENDING_PAYMENT` forever because the confirmation webhook never arrives. Use the
> **simulate** step in Section 4 instead if you don't want a tunnel.

### 2.6 Test card

In the hosted sandbox checkout use Paddle's test card:

```
4242 4242 4242 4242   any future expiry   any CVC   any postal code
```

> With these values collected, set the `PADDLE_*` env vars in the billing service (step 1.3)
> and **restart it** so checkout and webhook verification work.

---

## 3. Configure `tnra-app` (the group app, :8080)

`tnra-app` runs locally exactly as it does today (Keycloak + MySQL via the dev
`docker-compose.yml`, the app itself from your IDE / `./mvnw`). Billing adds three
settings and they are **off by default**. These settings are **provider-agnostic** — they
don't change between Lemon Squeezy and Paddle.

### 3.1 Settings overview

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

# Where Paddle returns the buyer after a successful checkout (tnra.app.base-url). Set it to
# your local app so you're returned to /billing/activating here:
export APP_BASE_URL=http://localhost:8080
```

> `api-token` is required when `enabled=true`. The `BillingClientImpl` bean only exists
> when `tnra.billing.enabled=true`, and `@Value("${tnra.billing.api-token}")` will fail
> fast on startup if it is missing — so register the group (step 3.3) **before** you
> start `tnra-app` with billing on.

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
`PADDLE_*` values (steps 1.3 + Section 2) → register the group (step 3.3) → **tnra-app
(:8080)** with the token → **tunnel (step 2.5)**.

1. Log into `tnra-app` as a member (e.g. `http://localhost:8080`, OG `tnra` realm user).
2. Because the group has no trial/exempt and the member has no subscription, the
   `EntitlementGate` forwards any protected route to **`/billing`** ("Your membership").
3. Click **Pay $7 / month** → you're sent to the hosted Paddle checkout. Pay with the test
   card (step 2.6).
4. On success, Paddle returns you to **`/billing/activating`** ("Activating your
   membership…"). Meanwhile it fires `subscription.created` / `transaction.completed` to
   your tunnel → `/api/billing/webhook` → the account flips to `ACTIVE`.
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

# inspect rows (columns are ls_* but hold Paddle ids)
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" tnra_billing \
  -e "SELECT email,status,payer_email,ls_subscription_id FROM billing_account;
      SELECT event_name,processed,error FROM billing_webhook_event ORDER BY id DESC LIMIT 5;"
```

8. **Manage subscriptions.** "Update payment method" on `/billing` opens the hosted Customer
   Portal (`/api/v1/portal` → Paddle `POST /customers/{id}/portal-sessions` →
   `data.urls.general.overview`), where you can update a card or cancel. If you're covering
   anyone, `/billing` also shows a **"Members you're covering"** list. The portal link works
   even for a **pure gifter** (a trial/comp member with no subscription of their own).

> If after paying the member is still sent to the membership page, the webhook didn't
> arrive — check `SELECT status FROM billing_account` (still `PENDING_PAYMENT`) and
> `SELECT * FROM billing_webhook_event` (empty). The redirect back to the app and the
> webhook are independent; only the webhook flips entitlement. Either fix delivery
> (tunnel + destination URL, step 2.5) or simulate it locally (below).

### Simulate the webhook locally (no tunnel)

If you don't want to run a public tunnel, POST a synthetic `subscription.created` event
straight to the billing app. The account row already exists (`PENDING_PAYMENT`) from the
checkout, so this flips it to `ACTIVE` exactly as a real Paddle webhook would.

Paddle signs with `Paddle-Signature: ts=<unix>;h1=<hex>` where
**`h1 = HMAC-SHA256("<ts>:<rawBody>", PADDLE_WEBHOOK_SECRET)`** (note: `ts:` is prefixed to
the body before hashing — different from Lemon Squeezy, which signed the body alone). Run
this in the **same shell where `PADDLE_WEBHOOK_SECRET` is exported**, and set `custom_data`
to match the account you created (group slug + beneficiary email):

```bash
TS=$(date +%s)
BODY='{"event_type":"subscription.created","data":{"id":"sub_sim1","status":"active","customer_id":"ctm_sim1","custom_data":{"group_slug":"local","beneficiary_email":"member@example.com","payer_email":"member@example.com"}}}'
H1=$(printf '%s' "$TS:$BODY" | openssl dgst -sha256 -hmac "$PADDLE_WEBHOOK_SECRET" | awk '{print $NF}')
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8082/api/billing/webhook \
  -H "Content-Type: application/json" \
  -H "Paddle-Signature: ts=$TS;h1=$H1" \
  --data-binary "$BODY"
# → 200   (401 = PADDLE_WEBHOOK_SECRET mismatch or malformed Paddle-Signature)
```

Re-check entitlement (`status` should now be `ACTIVE`) and reload `tnra-app` (allow the 60s
cache or restart). Vary the event to exercise the rest of the matrix:

- `data.status` `past_due` → `ON_GRACE_PERIOD`; `paused`/`canceled` → `SUSPENDED`.
- `event_type` `transaction.payment_failed` or `subscription.past_due` → `ON_GRACE_PERIOD`.
- `event_type` `subscription.canceled` or `subscription.paused` → `SUSPENDED`.
- `event_type` `transaction.completed` / `subscription.activated` / `subscription.resumed` → `ACTIVE`.

### Things to try

- **Profile → Billing tab**: everything in one place — membership status (**pay options shown
  only when you have no active subscription**, **"Update payment method"** always shown), a
  **"Gift a membership"** section, and a **"Members you're covering"** list. The tab's
  billing-service calls are **lazy** — they only fire when you open the Billing tab.
- **Gift a membership (UI)**: as an **active** member, open **Profile → Billing → Gift a
  membership**, pick another member, and click **Continue to checkout** → you land on
  `/billing` in gift mode. Choosing a plan sends you to Paddle with the **beneficiary's**
  email in `custom_data`; you (the payer) are charged, and the webhook activates *their*
  account. A beneficiary who is **already settled** (active sub, trial, exempt, or an
  existing gift) is blocked with **409**; a beneficiary in **`ON_GRACE_PERIOD`** is
  intentionally still **giftable** (a rescue that supersedes the failing subscription).
- **Gift supersede**: a beneficiary who self-pays to replace a gift creates a NEW
  subscription; its `subscription.created` routes by `custom_data`, and the old gift
  subscription is cancelled via `POST /subscriptions/{id}/cancel`.
- **Gift / covering (API)**: a gift is `payerEmail` ≠ `beneficiaryEmail`.
  `GET /api/v1/covering?payerEmail=...` lists who a payer covers. A `billing_webhook_event`
  with a non-null `error` and `processed=false` is an **unmatched** event kept for
  reconciliation — check the `custom_data` routing.

### Troubleshooting

| Symptom | Likely cause |
|---------|--------------|
| `tnra-billing-app` won't start; `Unknown database 'tnra_billing'` | Database not created — do step 1.1 before first boot. |
| Paywall never shows; member always gets in | Group is exempt or in trial (`GROUP_EXEMPT`/`GROUP_TRIAL`), or billing service is down and the gate is failing open. Re-register with `trialDays:0, exempt:false`; confirm `:8082` is UP. |
| `tnra-app` won't start with billing on | `TNRA_BILLING_API_TOKEN` missing/blank while `enabled=true`. Register the group first, paste the token. |
| Checkout 500 / "couldn't start checkout" | Bad `PADDLE_API_KEY` / Price id, live-mode key used against the sandbox base (or vice-versa), or **no default payment link configured** (step 2.3) so `data.checkout.url` is absent. |
| Account stuck `PENDING_PAYMENT` after paying | Webhook never reached the service — tunnel down, wrong destination URL, or `PADDLE_WEBHOOK_SECRET` mismatch (401, nothing persisted). |
| Paddle shows the webhook failing with **401** | Signature rejected. The destination's **secret key** in Paddle must equal the billing app's `PADDLE_WEBHOOK_SECRET`, and it must be **non-blank** (a blank secret rejects every webhook). The signed string is `"<ts>:<body>"`, not the body alone. Set both, restart the billing app. |
| `401 Admin API not configured` on register | `BILLING_ADMIN_TOKEN` blank in the billing app, or the `X-Admin-Token` header doesn't match. |
| Entitlement slow to change | 60s client cache in `tnra-app`; wait or restart. |

> **Note on approval:** Paddle won't let you fully complete a **live** checkout until the
> seller account **and its domain** are approved. Sandbox works without approval. For the
> live domain-review requirement (pricing must be visible on the **apex** homepage, not just
> `/pricing`), see the pricing changes shipped on the 9.2.x / 10.x landing site.
