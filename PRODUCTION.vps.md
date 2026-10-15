# Production Deployment Guide

This guide covers deploying TNRA to a VPS (e.g., Vultr) running Docker.

## Initial Server Setup

### 1. Provision and update the server

```bash
apt-get update && apt-get upgrade -y
```

### 2. Create an unprivileged user

```bash
adduser tnra
usermod -aG docker tnra
```

### 3. Install dependencies

```bash
apt install -y docker.io docker-compose git
```

Verify Docker is running:

```bash
systemctl enable docker
systemctl start docker
```

### 4. Clone the repository

```bash
su - tnra
git clone https://github.com/dogeared/tnra
cd tnra
```

### 5. Bootstrap

Run the interactive setup script. It prompts for all secrets, writes `.env`, and starts MySQL and Keycloak:

```bash
bash bootstrap.sh
```

The script will:
- Prompt for your domain, MySQL root password, Keycloak admin credentials, and encryption master key (auto-generating any you leave blank)
- Write a complete `.env` file
- Start MySQL and Keycloak via `docker-compose.production.yml`
- Wait for MySQL to be healthy
- Print all generated credentials — **save them in a password manager before continuing**

After first boot, rotate the Keycloak client secret for each group (the realm import includes a placeholder):

1. Open an SSH tunnel: `ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>`
2. Open `http://localhost:8180/admin` and log in with the Keycloak admin credentials.
3. Switch to the group realm → **Clients** → `<group>-app` → **Credentials** tab.
4. Click **Regenerate** next to Client secret and copy the new value.
5. Update `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_SECRET` in `provision/<group-name>/docker-compose.yml` on the VPS.
6. Restart: `docker compose -f docker-compose.production.yml -f provision/<group-name>/docker-compose.yml up -d`

## SSL Certificates

Nginx requires SSL certificates at `nginx/.cert/cert.pem` and `nginx/.cert/key.pem`.

### Using certbot with Cloudflare DNS

On your local machine (or any machine with certbot):

```bash
brew install certbot  # or apt install certbot
pip3 install certbot-dns-cloudflare
```

Create `~/cloudflare-creds.ini`:

```ini
dns_cloudflare_api_token = <your_cloudflare_api_token>
```

```bash
chmod 600 ~/cloudflare-creds.ini
```

Generate the certificate:

```bash
certbot certonly \
  --config-dir ~/letsencrypt/config \
  --work-dir ~/letsencrypt/work \
  --logs-dir ~/letsencrypt/logs \
  --dns-cloudflare \
  --dns-cloudflare-credentials ~/cloudflare-creds.ini \
  -d tnra.afitnerd.com
```

Copy certs to the VPS:

```bash
scp ~/letsencrypt/config/live/tnra.afitnerd.com/fullchain.pem \
    ~/letsencrypt/config/live/tnra.afitnerd.com/privkey.pem \
    tnra@<VPS_IP>:~/
```

On the VPS, move certs into place:

```bash
mv ~/privkey.pem ~/tnra/nginx/.cert/key.pem
mv ~/fullchain.pem ~/tnra/nginx/.cert/cert.pem
```

### Certificate Renewal

Re-run the certbot command above and re-copy the files. Then restart nginx:

```bash
docker compose restart proxy
```

## Encryption at Rest

All sensitive post content (text fields, stat values, stat names, and emoji) is encrypted
using AES-256-GCM before being written to MySQL. The encryption is transparent to the
application — `AttributeConverter` implementations handle it on every JPA read/write.

### How it works

- On first startup, a 256-bit Data Encryption Key (DEK) is randomly generated, encrypted
  with the master key, and stored in the `encryption_keys` table.
- All subsequent reads and writes use the in-memory DEK; only the master key (not the DEK)
  needs to be kept outside the database.
- Flyway migrations V7–V10 handle schema changes and in-place encryption of existing data.

### Rotating the master key

Rotation requires decrypting the stored DEK with the old master key, re-encrypting it with
the new master key, and updating `encryption_keys` before restarting the app. There is no
automated tooling for this yet — contact the maintainer before attempting a rotation.

## Slack Notifications

Slack activity notifications are configured per-group through the **Admin → Settings** tab
— there are no server-side env vars for Slack.

1. Create an incoming webhook in your Slack workspace (Apps → Incoming Webhooks).
2. Log in as an admin and navigate to **Admin → Settings**.
3. Paste the webhook URL (must start with `https://hooks.slack.com/`) and enable the toggle.
4. When a member finishes a post, a notification is sent asynchronously with their display
   name, timestamps, and an encrypted deep-link URL to the post.

The SSRF guard rejects any webhook URL that does not begin with `https://hooks.slack.com/`,
so a compromised settings record cannot redirect notifications to an external server.

## Build and Deploy

### First deployment

Build the app JAR (the per-group containers bake `tnra-app/target/*.jar` via their `./tnra-app`
build context):

```bash
cd ~/tnra
./mvnw -pl tnra-app -am clean package -DskipTests -Pproduction
```

Then bring everything up — start the shared infrastructure, then each provisioned group. See
[Bringing the Stack Up](#bringing-the-stack-up) for the one-liners.

### Deploy the landing service

The landing service (`tnra-landing`) runs as a separate container in `docker-compose.production.yml`.
Build and start it after the shared services (MySQL) are healthy.

Add to `~/tnra/.env` before deploying:
```
MAILGUN_KEY_PRIVATE=<your Mailgun private key>
MAILGUN_URL=https://api.mailgun.net/v3/<active sending domain>/messages
TNRA_FOUNDER_EMAIL=<your email address>
# Optional. Sender; keep it on the sending domain above so SPF/DKIM align (default tnra-landing@tnra.app).
TNRA_NOTIFY_FROM=
```

A submission is saved first and the email sent after, so a Mailgun failure never loses the request —
it logs `Failed to send Mailgun notification` with Mailgun's reason (a 403 "not allowed to send" means
the sending domain is suspended or unverified; a 401 means the key). Reproduce outside the app with
`curl -u "api:$MAILGUN_KEY_PRIVATE" "$MAILGUN_URL" -F from=… -F to=… -F subject=x -F text=x`.

Then build the JAR (requires the production Vaadin frontend bundle) and deploy:

```bash
cd ~/tnra
./mvnw -pl tnra-landing-app -am clean package -Pproduction -DskipTests
docker build -t tnra-landing:latest tnra-landing-app/
docker compose -f docker-compose.production.yml up -d tnra-landing
```

The landing app's `application.yml` is for local dev only — production does not use it. All
config the container needs is supplied via env in `docker-compose.production.yml`
(`SPRING_DATASOURCE_*`, `TNRA_NOTIFY_*`, `VAADIN_PRODUCTIONMODE`); everything else falls back to
Spring defaults (including `server.port` = 8080, matching the port mapping and proxy target).

Copy the nginx config and reload:
```bash
cp nginx/sites-disabled/tnra-landing.conf /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

Verify:
```bash
docker logs tnra-landing
# Look for: "Started TnraLandingApplication"
curl -sk https://tnra.app | grep -o "<title>.*</title>"
```

### Updating the landing service

The landing site is a separate container and image from the per-group app, so it can ship on
its own cadence when only the public pages change. Its page copy is bundled in the jar
(`tnra-landing-app/src/main/resources/content/*.md`), so even a content-only edit needs a
rebuild. `--no-deps` recreates just the landing container and leaves MySQL, Keycloak, and
every group container running:

```bash
cd ~/tnra
git pull origin main
./mvnw -pl tnra-landing-app -am clean package -Pproduction -DskipTests
docker build -t tnra-landing:latest tnra-landing-app/
docker compose -f docker-compose.production.yml up -d --no-deps tnra-landing
```

### Subsequent deployments

```bash
cd ~/tnra
git pull origin main

./mvnw -pl tnra-app -am clean package -DskipTests -Pproduction

# Rebuild and restart every provisioned group (shared infra stays up)
for g in provision/*/; do
  [ -f "${g}docker-compose.yml" ] || continue
  docker compose -f docker-compose.production.yml -f "${g}docker-compose.yml" up -d --build
done

# Verify a group's startup (replace <group-name>)
docker logs tnra-<group-name> -f
# Look for: "Started TnraApplication" and any Flyway migration messages
```

## Bringing the Stack Up

The deployment is **two layers**: one set of **global shared services** defined in
`docker-compose.production.yml`, and **one container per group** under `provision/<group>/`.
Bring the shared layer up first — it creates the `tnra-shared` Docker network that
every group container attaches to.

### Global / shared infrastructure (one-liner)

```bash
cd ~/tnra
docker compose -f docker-compose.production.yml up -d
```

Starts MySQL, Keycloak, and the `tnra-landing` site. Build the landing image once before the
first run (see [Deploy the landing service](#deploy-the-landing-service)). To start only the
data plane (no landing site), scope it:

```bash
docker compose -f docker-compose.production.yml up -d mysql keycloak
```

The **central billing service** (`tnra-billing`) is opt-in behind the `billing` profile, so a bare
`up` never starts it. To include it, add `--profile billing` (or target it directly). See
[Central Billing Service](#central-billing-service-paddle) for setup; in short:

```bash
# Shared infra + landing + billing, all at once:
docker compose -f docker-compose.production.yml --profile billing up -d
```

### All provisioned sites in `provision/`

Build the app JAR once — the group containers bake `tnra-app/target/*.jar` via their `./tnra-app`
build context — then loop over every provisioned group:

```bash
cd ~/tnra
./mvnw -pl tnra-app -am clean package -DskipTests -Pproduction

for g in provision/*/; do
  [ -f "${g}docker-compose.yml" ] || continue
  docker compose -f docker-compose.production.yml -f "${g}docker-compose.yml" up -d --build
done
```

Bring up (or restart) a single site by naming it:

```bash
docker compose -f docker-compose.production.yml -f provision/<group-name>/docker-compose.yml up -d --build
```

## Running as a systemd Service

The repo includes `tnra.service` for running via systemd.

```bash
# Create symlink (as root)
ln -s /home/tnra/tnra/tnra.service /etc/systemd/system/tnra.service

# Enable and start
systemctl enable tnra.service
systemctl start tnra.service
```

The service runs `tnra.start.sh`, which sources `.env`, brings up the shared infrastructure
(adding `--profile billing` when `BILLING_ADMIN_TOKEN` is set), then starts
every group under `provision/`. `tnra.stop.sh` stops them all.

```bash
# Check status
systemctl status tnra.service

# View a group's logs (replace <group-name>)
docker logs tnra-<group-name> -f

# View shared-infra logs
docker compose -f docker-compose.production.yml logs -f

# Stop
./tnra.stop.sh  # or: systemctl stop tnra.service
```

## Firewall Configuration

Allow HTTPS (port 443) and SSH. Block all other inbound ports:

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 443/tcp
ufw enable
```

MySQL (3307) and Keycloak (8180) are bound to `127.0.0.1` in `docker-compose.yml` and are
not exposed externally.

## Hardening MySQL

Root password and app-user password setup are covered in steps 6 and 7 of
[Initial Server Setup](#initial-server-setup). The items below cover ongoing hardening.

### MySQL data persistence

MySQL data is stored in the `mysql-db` Docker volume. Back up regularly:

```bash
docker compose exec mysql mysqldump -uroot -p<root_password> \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql
```

### Remote access

The MySQL container is bound to `127.0.0.1:3307` in `docker-compose.yml` and is not
reachable from outside the host. Keep it this way. Access the MySQL shell via:

```bash
docker compose exec mysql mysql -utnra -p<app_password> tnra
```

## Keycloak (central, separate repo)

Since 10.7.0 Keycloak is not part of this stack: it is the central instance in the **dogeared/keycloak**
repo (compose project `keycloak-central`, bound to `127.0.0.1:8180`, joined to `tnra-shared` under the
alias `keycloak`). Admin access, hardening, backups and production mode are documented there.

## Central Billing Service (Paddle)

`tnra-billing-app` is a **single central instance** shared by every group — it is the
source of truth for entitlement and the only component that talks to Paddle
(Merchant of Record). It is optional: a self-host deploy can leave it off and every group
runs the full app with no payment surface. Run it only for the paid SaaS deploy.

> Local end-to-end testing of this whole flow is documented separately in
> `BILLING_PLAN_LOCAL_TESTING.md`.

### Architecture

```
  group-a.tnra.app ─► tnra-a:8080 ──┐  Bearer <per-group token>
  group-b.tnra.app ─► tnra-b:8080 ──┤
                                    ▼
                          tnra-billing:8082  ──► Paddle Billing API
                              │  tnra_billing DB (shared MySQL)
  Paddle ── webhook (Paddle-Signature) ──► https://billing.tnra.app/api/billing/webhook
```

The checkout itself opens on the **landing site**: the group app redirects the member to
`https://tnra.app/pay?_ptxn=…`, Paddle.js there shows the overlay, and on completion the page
sends them back to the group's `/billing/activating`. So the landing container needs the
`PADDLE_CLIENT_TOKEN` / `PADDLE_ENVIRONMENT` env too (already wired in the compose file).

Each group app reaches billing over its **public** HTTPS URL (`https://billing.tnra.app`),
authenticated by a per-group bearer token minted at provisioning. The Paddle
webhook must also be publicly reachable. So the billing service gets its own subdomain.

### Step 1: Paddle (live)

Mirror the sandbox setup in `BILLING_PLAN_LOCAL_TESTING.md` §2, but in the **live** Paddle
dashboard (<https://vendors.paddle.com> — a separate login from the sandbox, with its own
API key, Price ids, and webhook secret):

- **Catalog → Products**: one product with two recurring **Prices** — **Monthly ($7)** and
  **Yearly ($60)**. Copy each Price id (`pri_…`) → `PADDLE_PRICE_MONTHLY` /
  `PADDLE_PRICE_YEARLY`.
- **Developer Tools → Authentication**: create a live API key → `PADDLE_API_KEY`, and a
  live **client-side token** → `PADDLE_CLIENT_TOKEN` (set `PADDLE_ENVIRONMENT=production`).
- **Checkout → Checkout settings → Default payment link**: `https://tnra.app/pay`. Paddle's
  "hosted" checkout is not a Paddle-hosted page — the `checkout.url` the billing service
  gets back is this URL with `?_ptxn=txn_…` appended, and the landing app's `/pay` page
  (Paddle.js) opens the overlay for that transaction. Paddle only returns `checkout.url`
  when this is set, and in live it must be on the approved domain.
- **Developer Tools → Notifications**: a destination with URL
  `https://billing.tnra.app/api/billing/webhook`. Copy its secret key (`pdl_ntfset_…`) →
  `PADDLE_WEBHOOK_SECRET`. Subscribe it to `subscription.created`,
  `subscription.activated`, `subscription.updated`, `subscription.canceled`,
  `subscription.past_due`, `subscription.paused`, `subscription.resumed`,
  `transaction.completed`, and `transaction.payment_failed`.

> Live hosted checkout requires a verified Paddle seller account and an approved checkout
> domain. Live keys are distinct from sandbox keys. Never put sandbox Price ids in
> production — a misconfigured deploy charges the wrong account or fails to verify
> webhooks.

### Step 2: Environment

Add to `~/tnra/.env` (read by `docker-compose.production.yml` via `env_file`):

```bash
# --- Central billing (Paddle, Merchant of Record) ---
PADDLE_API_KEY=
# Must be the LIVE base in production. The template and compose default to the SANDBOX
# (https://sandbox-api.paddle.com), so this line is easy to miss.
PADDLE_API_BASE=https://api.paddle.com
# Notification destination secret key (pdl_ntfset_...)
PADDLE_WEBHOOK_SECRET=
# Paddle Price ids (pri_...) for the $7/mo and $60/yr plans
PADDLE_PRICE_MONTHLY=
PADDLE_PRICE_YEARLY=
# Paddle.js client-side token (live_...) for the landing app's /pay page + matching environment
PADDLE_CLIENT_TOKEN=
PADDLE_ENVIRONMENT=production
# Guards the provisioning/admin API (X-Admin-Token). Blank ⇒ admin API disabled (fail closed).
# Generate with: openssl rand -hex 24
BILLING_ADMIN_TOKEN=
# Default free-trial window (days) applied to a new group at registration.
TNRA_TRIAL_DAYS=60
```

### Step 3: Build and start the service

The `tnra-billing` service is **already defined** in `docker-compose.production.yml`, behind the
`billing` profile (so it's opt-in — a bare `up` never starts it). It joins the `tnra-shared` network
as `tnra-billing` on port 8082; only nginx publishes it externally. Its datasource defaults to the
shared MySQL container and **auto-creates** the `tnra_billing` schema on first connect
(`createDatabaseIfNotExist=true`), so there's no manual `CREATE DATABASE` step. Flyway builds the
tables on first boot.

`tnra-billing-app/Dockerfile` builds the jar image. Build and start it (from `~/tnra/`, after pulling
latest):

```bash
cd ~/tnra
./mvnw -pl tnra-billing-app -am package -DskipTests
docker build -t tnra-billing:latest tnra-billing-app
docker compose -f docker-compose.production.yml --profile billing up -d tnra-billing
docker logs tnra-billing -f   # look for "Started TnraBillingApplication" + Flyway migrations
```

> **Running individually or all at once.** Because billing is its own profiled service, you can run it
> on its own, alongside the landing site, or as part of the whole stack:
>
> ```bash
> # Everything (shared infra + landing + billing):
> docker compose -f docker-compose.production.yml --profile billing up -d
> # Just billing (+ its MySQL dependency):
> docker compose -f docker-compose.production.yml up -d tnra-billing
> # Just the landing site:
> docker compose -f docker-compose.production.yml up -d tnra-landing
> # Update only billing, leaving everything else running:
> docker compose -f docker-compose.production.yml up -d --no-deps tnra-billing
> ```
>
> **Future: separate VM/container.** When billing moves to its own host, run the same
> `--profile billing` service there and set `BILLING_DATASOURCE_URL` in that host's `.env` to its
> database (e.g. `jdbc:mysql://<db-host>:3306/tnra_billing?...`). Nothing else couples it to the
> landing host except the shared `tnra-shared` network, which each host creates on its own.

### Step 4: Nginx route for `billing.tnra.app`

The per-group API and the webhook are reached over HTTPS; both are token/HMAC
authenticated, so the whole host can be proxied. Add an Nginx site
(`nginx/sites/billing.conf`) that terminates TLS for `billing.tnra.app` and proxies to
`http://tnra-billing:8082`, then:

```bash
cd ~/tnra
docker compose -f docker-compose.production.yml restart proxy
curl https://billing.tnra.app/actuator/health   # {"status":"UP"}
```

Add DNS (`A` record for `billing.tnra.app`, or the existing `*.tnra.app` wildcard covers
it) and include it in your wildcard/SAN certificate.

### Step 5: Verify

```bash
# admin API reachable (expects 200 + token shown once)
curl -s -X POST https://billing.tnra.app/api/admin/groups \
  -H "X-Admin-Token: $BILLING_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"groupSlug":"smoketest","trialDays":0,"exempt":true}'
```

Then confirm the webhook path end to end: in the Paddle dashboard open the notification
destination and send a **test event**. The billing service must answer 2xx — a 401 means
`PADDLE_WEBHOOK_SECRET` does not match the destination's secret key.

> **Existing live group (do this before enabling billing on it).** Members of a group that
> is already running compute as `PENDING_PAYMENT` the moment billing is switched on, unless
> the group is covered. Either leave `TNRA_BILLING_ENABLED` unset in that group's compose
> file (no gate, no payment surface), or register it as a forever-free pilot first — the
> same `POST /api/admin/groups` call above with the group's real slug and `"exempt":true`
> — and only then add the `TNRA_BILLING_*` env to its compose file.

New groups are wired to billing during provisioning — see **Provisioning New Groups →
Step 2** below (`--billing-api-url` / `--billing-admin-token`).

---

## Provisioning New Groups

Each TNRA group runs as its own Docker container with its own MySQL database, Keycloak
realm, and subdomain. The `tnra-cli` tool generates all config files. The operator applies
them manually.

### Architecture

```
                    ┌──────────────────────────────────────┐
                    │           Shared VPS                  │
                    │                                       │
  group-a.tnra.app ─►│  Nginx ──► tnra-group-a:8080        │
  group-b.tnra.app ─►│        ──► tnra-group-b:8080        │
                    │                                       │
                    │  MySQL (shared instance)              │
                    │    ├── tnra_group_a (database)        │
                    │    └── tnra_group_b (database)        │
                    │                                       │
                    │  Keycloak (shared instance)           │
                    │    ├── group-a (realm)                │
                    │    └── group-b (realm)                │
                    └──────────────────────────────────────┘
```

All containers share the `tnra-shared` Docker network. Each group's container connects to
MySQL and Keycloak using internal Docker hostnames (`mysql`, `keycloak`).

### Step 1: Build the CLI (on your local machine)

```bash
cd tnra-cli-app && mvn package -DskipTests && cd ..
```

### Step 2: Provision

```bash
java -jar tnra-cli-app/target/tnra-cli.jar provision <group-name> --domain tnra.app
```

Replace `tnra.app` with your production domain. This generates 6 files in
`provision/<group-name>/`.

**To enable billing for the group**, also pass the central billing URL + admin token.
This registers the group centrally, mints its per-group token, and writes the billing
env into the generated `docker-compose.yml` (`BILLING_ENABLED=true`, `BILLING_API_URL`,
`BILLING_API_TOKEN`):

```bash
java -jar tnra-cli-app/target/tnra-cli.jar provision <group-name> --domain tnra.app \
  --billing-api-url https://billing.tnra.app \
  --billing-admin-token "$BILLING_ADMIN_TOKEN" \
  [--trial-days 60] [--billing-exempt]
```

- Use the **public** billing URL — the group container reaches it the same way Paddle
  does, and the same value is embedded as the group app's `api-url`.
- `--billing-exempt` registers a forever-free pilot group; `--trial-days` overrides the
  default trial window. Omit both for a standard "trial then pay" group.
- Omit the billing flags entirely for a self-host / non-paid group (billing stays off).
- Registration happens **before** any local files are written, so a billing failure
  aborts the provision cleanly. The per-group token is shown once — keep the generated
  `docker-compose.yml` safe.

### Step 3: DNS

Add an `A` record for `<group-name>.tnra.app` pointing to your VPS IP. If using a
wildcard cert, a single `*.tnra.app` record works for all groups.

### Step 4: SSL certificate

Either use a wildcard certificate:

```bash
certbot certonly \
  --dns-cloudflare --dns-cloudflare-credentials ~/cloudflare-creds.ini \
  -d "*.tnra.app"
```

Or generate a per-subdomain cert and update the Nginx config to reference it.

### Step 5: Import the Keycloak realm (from your local machine)

Open an SSH tunnel so the Keycloak admin UI is reachable:

```bash
ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>
```

In your browser, open `http://localhost:8180/admin` and log in with your Keycloak admin credentials.

1. Click the realm dropdown (top-left) → **Create realm**.
2. Click **Browse** and select your local `provision/<group-name>/<group-name>-realm.json`.
3. Click **Create**.

Verify that the realm dropdown shows `<group-name>` and that the client `<group-name>-app` is configured with the correct redirect URIs.

### Step 6: Copy files to VPS

```bash
scp -r provision/<group-name> tnra@<VPS_IP>:~/tnra/provision/
```

This places the files at `~/tnra/provision/<group-name>/` on the VPS, mirroring the local structure.

### Step 7: Initialize the database

SSH into the VPS and run from `~/tnra/`:

```bash
cd ~/tnra
docker compose -f docker-compose.production.yml exec -T mysql mysql -uroot -p<root_password> \
  < provision/<group-name>/init-db.sql
```

### Step 8: Deploy the group's app container

All credentials are embedded in the generated `docker-compose.yml`. The encryption master key is
read via Docker Compose variable interpolation from `~/tnra/.env`, so the command must be run from
`~/tnra/`:

```bash
cd ~/tnra
docker compose -f docker-compose.production.yml -f provision/<group-name>/docker-compose.yml up --build -d
```

Watch logs:
```bash
docker logs tnra-<group-name> -f
```

Look for: `Started TnraApplication` and Flyway migration messages.

### Step 9: Configure Nginx

```bash
cd ~/tnra
cp provision/<group-name>/<group-name>.conf nginx/sites/
docker compose -f docker-compose.production.yml restart proxy
```

### Step 10: Create the first admin user

1. SSH tunnel: `ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>`
2. Open `http://localhost:8180/admin`
3. Switch to the `<group-name>` realm
4. **Users** → **Add user** (set email, first name, last name)
5. **Credentials** → Set password (disable "Temporary")
6. **Role Mappings** → Assign `admin` and `member` roles

### Step 11: Verify

Visit `https://<group-name>.tnra.app` and log in with the admin user.

### Managing multiple groups

```bash
# View all running group containers
docker ps --filter "name=tnra-"

# View logs for a specific group
docker logs tnra-<group-name> -f

# Restart a specific group
docker restart tnra-<group-name>

# Back up a specific group's database
docker compose -f docker-compose.production.yml exec mysql mysqldump -uroot -p<password> \
  --skip-column-statistics --no-tablespaces <db_name> \
  > ~/backups/<group-name>-$(date +%Y%m%d).sql
```

### Group registry

All provisioned groups are tracked in `groups.json` (gitignored, not checked in). Copy
`groups.json.example` to `groups.json` before first use. The CLI auto-assigns ports and
prevents duplicate group names. Keep this file backed up on the VPS.

### Migrating the existing group

The existing single-tenant deployment is registered as the first group in `groups.json`
(name: "tnra", port: 8080). After adding the `tnra-shared` network to `docker-compose.yml`
and restarting, it continues to work as before. No data migration required.

## Database Migration from V1

If you have an existing V1 database (pre-Flyway, with hardcoded embedded stats), follow this
process to migrate to the current schema.

### Overview

The migration path is:

```
V1 (baseline)
→ V2 (remove legacy columns)
→ V3 (configurable stats)
→ V4 (notification preferences)
→ V5 (personal stats + email unique)
→ V6 (post.state TINYINT → VARCHAR for Hibernate 6)
→ V7 (encryption_keys table + widen encrypted columns)
→ V8 (encrypt existing post/stat plaintext in-place)
→ V9 (widen stat_definition.emoji to TEXT)
→ V10 (encrypt existing emoji values in-place)
```

Flyway handles all of this automatically on startup with `baseline-on-migrate: true`.
**`TNRA_ENCRYPTION_MASTER_KEY` must be set before startup** — V8 and V10 are Java
migrations that read the master key at migration time.

### Step 1: Back up the existing database

```bash
mysqldump -h <host> -u<username> -p \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql
```

Verify the backup:

```bash
grep "CREATE TABLE" ~/tnra-backup-*.sql | wc -l
grep "INSERT INTO" ~/tnra-backup-*.sql | wc -l
```

### Step 2: Test migration locally

Restore the production backup to a local test database:

```bash
docker compose up mysql -d
docker compose exec mysql mysqladmin ping -h localhost --wait=30
docker compose exec -T mysql mysql -uroot -p<password> \
  -e "CREATE DATABASE IF NOT EXISTS tnra_migration_test;"
docker compose exec -T mysql mysql -uroot -p<password> tnra_migration_test \
  < ~/tnra-backup-*.sql
```

Run the app against the test database:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/tnra_migration_test \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=<password> \
./mvnw -pl tnra-app spring-boot:run
```

Watch logs for successful migration messages. Verify:

```sql
-- Connect: docker compose exec mysql mysql -uroot -p<password> tnra_migration_test

-- Check Flyway history
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
-- Should show V1–V10 all with success=1

-- Verify configurable stats migrated
SELECT COUNT(*) FROM stat_definition;   -- should be 7
SELECT COUNT(*) FROM post_stat_value;   -- should match your non-null stat count

-- Verify old embedded columns are gone
DESCRIBE post;
-- Must NOT have: exercise, gtg, meditate, meetings, pray, _read, sponsor

-- Verify encryption table exists and DEK was stored
SELECT COUNT(*) FROM encryption_keys;   -- should be 1

-- Verify post data is encrypted (values should start with ENC:)
SELECT widwytk FROM post LIMIT 1;
```

### Step 3: Deploy to production

```bash
cd ~/tnra
git pull origin main

./mvnw -pl tnra-app -am clean package -DskipTests -Pproduction

# Redeploy the group whose database you migrated (Flyway runs on startup); replace <group-name>
docker compose -f docker-compose.production.yml -f provision/<group-name>/docker-compose.yml up -d --build

docker logs tnra-<group-name> -f
# Verify: "Successfully applied N migrations" and "Started TnraApplication"
```

### Rollback

If migration fails: restore from backup and redeploy the previous version.

```bash
docker compose exec -T mysql mysql -uroot -p<password> tnra < ~/tnra-backup-*.sql
# Redeploy previous git tag/commit
```

The V3 stats migration drops the old embedded columns -- there is no partial rollback.
Always back up first.

## Helpful Commands

```bash
# View running shared-infra containers
docker compose -f docker-compose.production.yml ps

# View Keycloak logs
docker compose -f docker-compose.production.yml logs -f keycloak

# View MySQL logs
docker compose -f docker-compose.production.yml logs -f mysql

# Database shell
docker compose -f docker-compose.production.yml exec mysql mysql -uroot -p<password>

# Dump a group database
docker compose -f docker-compose.production.yml exec mysql mysqldump -uroot -p<password> \
  --skip-column-statistics --no-tablespaces <db_name> > ~/tnra-backup-$(date +%Y%m%d).sql

# Restart a shared service
docker compose -f docker-compose.production.yml restart keycloak

# Restart shared infrastructure
docker compose -f docker-compose.production.yml up -d

# View a group's app logs
docker logs tnra-<group-name> -f

# Restart a group app
docker restart tnra-<group-name>
```
