[![Build](https://github.com/dogeared/tnra/actions/workflows/tests.yml/badge.svg)](https://github.com/dogeared/tnra/actions/workflows/tests.yml)
[![Test Coverage](https://raw.githubusercontent.com/dogeared/tnra/badges/jacoco.svg)](https://github.com/dogeared/tnra/actions/workflows/tests.yml)
[![Version](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdogeared%2Ftnra%2Fmain%2FVERSION&search=%5B0-9.%5D%2B&label=version&color=2D6A4F)](https://github.com/dogeared/tnra/blob/main/CHANGELOG.md)

# TNRA - Structured Accountability for Groups

TNRA is a structured accountability group app. Members track daily/weekly/monthly/yearly
cadences: daily call chains, weekly posts (intro, kryptonite, commitments, personal/family/work
best & worst, stats), monthly meetings, and yearly retreats.

## Architecture

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Backend        | Spring Boot 4.1, Java 21           |
| Frontend       | Vaadin Flow 25.3 (server-side Java UI) — frontend builds need Node 24 |
| Database       | MySQL 8 with Flyway migrations     |
| Authentication | Keycloak 26 via OIDC/OAuth2 (PKCE) |
| Proxy          | Nginx (SSL termination, WebSocket)  |
| Runtime        | Docker Compose                      |

### Entity Model

```
User --(has many)--> Post
Post contains:
  - Intro (widwytk, kryptonite, whatAndWhen)
  - Category x3 (personal, family, work -- each with best/worst)
  - Stats (configurable via stat_definition + post_stat_value tables)
  - PostState (IN_PROGRESS or COMPLETE)
```

### File Uploads

Profile images are stored on the filesystem in the `uploads/` directory (not base64 in the
database). Files are UUID-named and served via `/uploads/{filename}`. Max size: 5MB, images only.

### Post View Modes

|                     | Completed View                                                                  | In Progress View                                                        |
|---------------------|---------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| In Progress Post    | - switch to in progress button - completed posts dropdown - pagination controls | - switch to completed button - post started date & time - finish button |
| No In Progress Post | - start new post button - completed posts dropdown - pagination controls        | XXXX                                                                    |

## Prerequisites

- Docker and Docker Compose
- Java 21 (for running outside Docker)
- Maven (or use the included `./mvnw` wrapper)
- Node 24 for Vaadin frontend builds (the Vaadin plugin downloads one into `~/.vaadin` if none is on
  the PATH). Vaadin's dev tooling lives in each web module's default-on Maven profile; `-Pproduction`
  builds leave it out.

## Local Development Setup

The dev model splits cleanly in two:

- **Infrastructure runs in Docker** — MySQL, Keycloak, and the Nginx proxy
  (`docker-compose.yml`). Nginx reaches the host-run apps via `host.docker.internal`.
- **The apps run from your IDE** — `tnra-app` on port **8080** and `tnra-landing-app` on
  port **8081**. Run/debug them straight from IntelliJ/VS Code, or via `./mvnw -pl <module> spring-boot:run`.
- **The CLI always runs from the command line** — `tnra-cli-app` (see
  [Running Multiple Groups Locally](#running-multiple-groups-locally)).

### 1. Clone the repository

```bash
git clone https://github.com/dogeared/tnra
cd tnra
```

### 2. Create your `.env` file

```bash
cp .env.template .env
```

At minimum, set:

```bash
MYSQL_ROOT_PASSWORD=<choose_a_root_password>
```

The `tnra` database user (matching `application.yml` defaults) is auto-created by
`mysql/init-local-user.sql` on first MySQL container start. Keycloak defaults work
out of the box for local dev (`tnra-app` / `tnra-app-secret` / `http://localhost:8180/realms/tnra`).

### 3. Start the infrastructure in Docker

```bash
# First run — start MySQL, Keycloak, and the Nginx proxy
docker compose up -d

# Or, restart existing containers from a previous run
docker compose start

# Wait for MySQL to be healthy
docker compose exec mysql mysqladmin ping -h localhost --wait=30
```

This brings up **only** the shared infrastructure — `docker-compose.yml` no longer defines any
app containers. MySQL is at `localhost:3307` (mapped from container `3306`); Keycloak's admin is
at `http://localhost:8180/admin`. On first container start, `mysql/init-local-user.sql` creates
the `tnra` user and both app databases (`tnra` and `tnra_landing`) so the main and landing apps
run zero-config from the IDE.

> **Note:** If you already have a MySQL Docker volume, the init script won't re-run. Either reset
> with `docker compose down -v && docker compose up -d`, or apply just the landing grant once:
> ```bash
> set -a && source ./.env && set +a
> docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
>   "CREATE DATABASE IF NOT EXISTS tnra_landing; GRANT ALL PRIVILEGES ON tnra_landing.* TO 'tnra'@'%'; FLUSH PRIVILEGES;"
> ```

### 4. SSL certificates and local DNS (for the Nginx proxy)

The proxy terminates TLS and routes the dev hostnames. Add them to `/etc/hosts`:

```
127.0.0.1 auth.dev.dogeared.dev tnra.dev.dogeared.dev landing.dev.dogeared.dev
```

Place certs in `nginx/.cert/` (a wildcard cert covers all three hostnames):

```bash
mkdir -p nginx/.cert
mkcert -install
mkcert -cert-file nginx/.cert/cert.pem -key-file nginx/.cert/key.pem \
  localhost auth.dev.dogeared.dev "*.dev.dogeared.dev"
```

### 5. Run the apps from your IDE

Each app's `application.yml` is gitignored (the repo ignores `*.yml`), so copy the tracked samples
once. The landing sample runs as-is against the dockerized MySQL (`tnra` user, port **8081**). The
main app's sample has placeholders for the DB credentials and the Vaadin dev-mode host allow-list,
so fill those in — without an `application.yml` the main app has no datasource at all, falls back
to in-memory H2, and fails on its MySQL-only Flyway migrations:

```bash
cp tnra-landing-app/src/main/resources/application.yml.sample \
   tnra-landing-app/src/main/resources/application.yml

sed -e 's/YOUR_DB_USERNAME/tnra/' -e 's/YOUR_DB_PASSWORD/123456aA$/' \
    -e 's/hostsAllowed: tnra.afitnerd.local/hostsAllowed: tnra.dev.dogeared.dev,localhost/' \
    tnra-app/src/main/resources/application.yml.sample > tnra-app/src/main/resources/application.yml
```

Three env vars are needed, and do **not** `source .env` to get them — it also exports blank
`SPRING_DATASOURCE_*` values that override the yml and silently switch both apps to H2:

```bash
# Post encryption key (set once in .env; the app refuses to start without it)
export TNRA_ENCRYPTION_MASTER_KEY="$(grep '^TNRA_ENCRYPTION_MASTER_KEY=' .env | cut -d= -f2-)"
# Keycloak advertises the proxy hostname as its issuer (KC_HOSTNAME in docker-compose.yml), and a
# browser must be sent there, so point the app at it — the localhost issuer in .env.template is for
# full-stack Docker. The JVM must trust the mkcert CA for this (see below).
export KEYCLOAK_ISSUER_URI=https://auth.dev.dogeared.dev/realms/tnra
# spring-boot:run forks the app with cwd = tnra-app/, so the relative `upload-dir: uploads` misses the
# repo-root uploads/ and every profile image (and the placeholder) 404s. Use the absolute path.
export APP_FILE_STORAGE_UPLOAD_DIR="$PWD/uploads"
```

The main app fetches Keycloak's discovery document over HTTPS at startup, and the JDK ships its own
trust store that knows nothing about mkcert. Import the CA once per JDK (the `keytool` on your
`PATH` must be the one `./mvnw -v` reports):

```bash
keytool -importcert -cacerts -storepass changeit -noprompt -alias mkcert-dev-ca \
  -file "$(mkcert -CAROOT)/rootCA.pem"
```

Then run both apps:

```bash
# Main app — http://localhost:8080  (proxied as https://tnra.dev.dogeared.dev)
./mvnw -pl tnra-app spring-boot:run

# Landing app — http://localhost:8081  (proxied as https://landing.dev.dogeared.dev)
./mvnw -pl tnra-landing-app spring-boot:run
```

In an IDE, run the `TnraApplication` and `TnraLandingApplication` main classes directly with the
same three env vars in the run configuration (an IDE run config usually uses the
repo root as working directory, in which case the upload-dir override is unnecessary). Both apps apply their Flyway migrations on first start
(`tnra`: V1–V12, `tnra_landing`: V1).

### 6. Create a test user in Keycloak

1. Go to `http://localhost:8180/admin` (admin/admin)
2. Select the `tnra` realm
3. Go to Users > Add user
4. Set username, email, first name, last name
5. Go to Credentials tab > Set password (disable "Temporary")
6. Log in to the app with this user

Pre-configured demo accounts (imported with realm):
- `jerry@seinfeld.com` / `heap9PUMPKIN@joanne` (admin + member)
- `kramer@seinfeld.com` / `heap9PUMPKIN@joanne` (member)
- `elaine@seinfeld.com` / `limbo5unfair!PRETTY` (member)
- `george@seinfeld.com` / `VETO7rugs@connors` (member)

### 7. Access the applications

| Service | Direct (IDE) | Via Nginx proxy |
|---|---|---|
| Main app | `http://localhost:8080` | `https://tnra.dev.dogeared.dev` |
| Landing app | `http://localhost:8081` | `https://landing.dev.dogeared.dev` |
| Keycloak admin | `http://localhost:8180/admin` | `https://auth.dev.dogeared.dev/admin` |

## Dev Site for Billing Testing (Paddle sandbox)

A private dev site for exercising the paid flow end to end — paywall, hosted checkout, test cards,
webhooks — without touching production or the public internet. It is the
[Local Development Setup](#local-development-setup) above plus the billing service, the landing
app's `/pay` page, and a tunnel for the one thing Paddle must reach: the webhook. The deep-dive
(entitlement matrix, simulated webhooks, troubleshooting) is `BILLING_PLAN_LOCAL_TESTING.md`;
this section is the checklist.

**Shape:** infra in Docker (MySQL, Keycloak, Nginx proxy with an mkcert cert), the three apps on
the host from the IDE/`./mvnw`, `/etc/hosts` for DNS. Nothing is reachable from the internet except
the webhook path, over a Cloudflare tunnel.

```
  browser (any LAN Mac) ─► https://tnra.dev.dogeared.dev     ─► proxy ─► tnra-app        :8080
                           https://landing.dev.dogeared.dev  ─► proxy ─► tnra-landing    :8081  (/pay)
                           https://auth.dev.dogeared.dev     ─► proxy ─► keycloak
  tnra-app ──────────────► http://localhost:8082 (tnra-billing) ──► Paddle sandbox API
  Paddle sandbox ── webhook ─► https://<tunnel-host>/api/billing/webhook ─► tnra-billing :8082
```

### 1. Base stack

Complete steps 2–5 of [Local Development Setup](#local-development-setup) (`.env`, Docker infra,
`/etc/hosts`, mkcert, **both** apps' `application.yml`, the mkcert CA imported into the JDK). Then
add the billing pieces:

```bash
cp tnra-billing-app/src/main/resources/application.yml.sample \
   tnra-billing-app/src/main/resources/application.yml

# The billing service owns its own schema; the MySQL init script only creates tnra + tnra_landing.
set -a && source .env && set +a
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
  "CREATE DATABASE IF NOT EXISTS tnra_billing; GRANT ALL PRIVILEGES ON tnra_billing.* TO 'tnra'@'%'; FLUSH PRIVILEGES;"
```

> **Proxy "Started" but not running?** `docker compose up -d` reports the proxy as started even
> when nginx exits a second later. `docker logs proxy` saying `cannot load certificate
> /etc/nginx/certs/cert.pem` means the mkcert step hasn't produced `nginx/.cert/cert.pem` yet.
> Generate the cert, then `docker compose up -d proxy`.

### 2. Testing from another machine on the LAN

The dev site is private by construction — only machines that resolve the hostnames can reach it.
To browse from a second Mac:

1. **Hosts** — point all three names at the dev machine's LAN IP (the landing hostname matters:
   the Paddle checkout page lives there):
   ```
   <dev-machine-ip> auth.dev.dogeared.dev tnra.dev.dogeared.dev landing.dev.dogeared.dev
   ```
2. **Trust the dev machine's mkcert CA** — do *not* generate a new CA; copy the public root
   (`rootCA.pem` from `mkcert -CAROOT` on the dev machine — never `rootCA-key.pem`) and trust it:
   ```bash
   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain rootCA.pem
   # or, with mkcert installed:  CAROOT=<dir containing rootCA.pem> mkcert -install
   ```
   Firefox keeps its own store: import the same file under Privacy & Security → Certificates.
3. macOS may prompt to allow Docker incoming connections the first time; allow it. Keycloak
   itself stays bound to `127.0.0.1:8180` on the dev machine — browsers only ever use the proxy.

### 3. Webhook tunnel

Payment confirmation arrives **only** by webhook; without one every purchase sits at
`PENDING_PAYMENT`. Paddle must reach `tnra-billing` (`:8082`) from the internet, so expose exactly
that — nothing else on the dev site is tunnelled. `brew install cloudflared`, then either:

- **Quick tunnel** (no account; new hostname **every run**, so you re-edit the Paddle notification
  destination each session):
  ```bash
  cloudflared tunnel --url http://localhost:8082
  ```
- **Named tunnel** (recommended — stable hostname, and only the webhook path is routed; everything
  else on that hostname is a 404). One-time setup; it is independent of any tunnel on other
  machines in the same Cloudflare account as long as the hostname is unique:
  ```bash
  cloudflared tunnel login
  cloudflared tunnel create tnra-dev
  cloudflared tunnel route dns tnra-dev billing-dev.dogeared.dev
  ```
  ```yaml
  # ~/.cloudflared/config.yml
  tunnel: tnra-dev
  credentials-file: /Users/<you>/.cloudflared/<tunnel-id>.json
  ingress:
    - hostname: billing-dev.dogeared.dev
      path: ^/api/billing/webhook$
      service: http://localhost:8082
    - service: http_status:404
  ```
  ```bash
  cloudflared tunnel run tnra-dev      # keep this terminal open while testing
  ```

### 4. Paddle sandbox

The sandbox is a **separate account and login** from live (<https://sandbox-vendors.paddle.com>),
with its own API key, Price ids, client-side token, and webhook secret. Nothing you do there
touches the live seller account. Collect:

| Where in the sandbox dashboard | Value | Goes to |
|---|---|---|
| Catalog → Products → one product, two recurring Prices ($7 monthly, $60 yearly) | `pri_…` ×2 | billing service: `PADDLE_PRICE_MONTHLY`, `PADDLE_PRICE_YEARLY` |
| Developer Tools → Authentication → API key | key | billing service: `PADDLE_API_KEY` |
| Developer Tools → Authentication → Client-side token | `test_…` | **landing app**: `PADDLE_CLIENT_TOKEN` |
| Checkout → Checkout settings → Default payment link | `https://landing.dev.dogeared.dev/pay` | (dashboard only — this is the page Paddle.js opens the overlay on; sandbox needs no domain approval and Paddle never fetches it) |
| Developer Tools → Notifications → new destination, URL `https://<tunnel-host>/api/billing/webhook` | `pdl_ntfset_…` secret | billing service: `PADDLE_WEBHOOK_SECRET` |

Subscribe the destination to: `subscription.created`, `.activated`, `.updated`, `.canceled`,
`.past_due`, `.paused`, `.resumed`, `transaction.completed`, `transaction.payment_failed`.

### 5. Run the three apps

Three terminals, in this order (the group app refuses to start with billing on until it has a
per-group token, which registration mints).

```bash
# ── 1. Billing service (:8082) ──────────────────────────────────────────────
export BILLING_ADMIN_TOKEN=$(openssl rand -hex 24); echo "$BILLING_ADMIN_TOKEN"
export PADDLE_API_KEY=...  PADDLE_WEBHOOK_SECRET=pdl_ntfset_...
export PADDLE_PRICE_MONTHLY=pri_...  PADDLE_PRICE_YEARLY=pri_...
# PADDLE_API_BASE defaults to the sandbox — leave it unset here.
./mvnw -pl tnra-billing-app spring-boot:run

# ── register the dev group with the trial OFF (else the 60-day trial hides the paywall) ──
curl -s -X POST http://localhost:8082/api/admin/groups \
  -H "X-Admin-Token: $BILLING_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"groupSlug":"local","trialDays":0,"exempt":false}'
# → copy "apiToken" from the response; it is shown once

# ── 2. Group app (:8080) ────────────────────────────────────────────────────
# Encryption key from .env (do NOT `source .env` — it also exports blank SPRING_DATASOURCE_* that
# silently switch the app to H2) and the https issuer the browser must be sent to.
export TNRA_ENCRYPTION_MASTER_KEY="$(grep '^TNRA_ENCRYPTION_MASTER_KEY=' .env | cut -d= -f2-)"
export KEYCLOAK_ISSUER_URI=https://auth.dev.dogeared.dev/realms/tnra
# spring-boot:run forks the app with cwd = tnra-app/, so the relative upload-dir misses the repo-root
# uploads/ (profile images + placeholder 404). Point it at the absolute path.
export APP_FILE_STORAGE_UPLOAD_DIR="$PWD/uploads"
export TNRA_BILLING_ENABLED=true
export TNRA_BILLING_API_URL=http://localhost:8082
export TNRA_BILLING_API_TOKEN=<apiToken from above>
export TNRA_APP_BASE_URL=https://tnra.dev.dogeared.dev   # must be https — /pay only follows https return links
./mvnw -pl tnra-app spring-boot:run

# ── 3. Landing app (:8081) — serves /pay ────────────────────────────────────
export PADDLE_CLIENT_TOKEN=test_...
export PADDLE_ENVIRONMENT=sandbox
./mvnw -pl tnra-landing-app spring-boot:run
```

### 6. Smoke test

A fresh `tnra` database has no members — Keycloak authenticates the demo accounts, but the app then
shows **"Not a Member"** until they exist in its `users` table (production groups get this from the
CLI's `seed-admin.sql`). Seed the four demo members and their default profile images (the PNGs in
`tnra-cli-app/src/main/resources/templates/`, copied into `uploads/`) once; it is safe to re-run:

```bash
./seed-dev.sh
```

1. Open `https://tnra.dev.dogeared.dev`, log in (e.g. `jerry@seinfeld.com`). The entitlement gate
   sends you to the membership page.
2. Click **Pay $7 / month**. You land on `https://landing.dev.dogeared.dev/pay?_ptxn=…` with the
   Paddle overlay open. Pay with the test card `4242 4242 4242 4242`, any future expiry/CVC/postcode.
3. After Paddle's success screen the page returns you to `/billing/activating`; the webhook flips
   the account `ACTIVE` and you are forwarded into the app. Entitlement is cached ~60s.

> Keeping all of these in one gitignored file (e.g. `~/.tnra-dev/billing.env`) and
> `set -a && source ~/.tnra-dev/billing.env && set +a` in each terminal saves re-typing them.

Checks when it doesn't: the browser console on `/pay` for **CSP** violations (the directive named
is the one to extend in the landing `SecurityConfig`); the billing log for the webhook landing with
a 2xx (**401** = `PADDLE_WEBHOOK_SECRET` mismatch); and if the account stays `PENDING_PAYMENT`, the
tunnel/destination URL. `BILLING_PLAN_LOCAL_TESTING.md` §4 has a curl that simulates the webhook
without a tunnel, and PATCH calls to flip the group between trial / exempt / must-pay.

## Running Multiple Groups Locally

Each TNRA group runs as its own container with its own database and Keycloak realm. The
`tnra-cli` tool generates all the config files you need to provision a new group.

Group containers join the **same dockerized dev infrastructure** described above (MySQL,
Keycloak, and the Nginx proxy on the `tnra-shared` network), so each group is reachable over
HTTPS at `https://<group>.dev.dogeared.dev`.

### Prerequisites

- The dev infrastructure running: `docker compose up -d` (mysql, keycloak, proxy). The group
  containers attach to its `tnra-shared` network.
- `TNRA_ENCRYPTION_MASTER_KEY` set in the **repo-root `.env`** — group containers read it from
  there via compose interpolation. Generate one with `openssl rand -base64 32` if needed.
- [mkcert](https://github.com/FiloSottile/mkcert) installed for local HTTPS
- Java 21 (to run the CLI)

### 1. Build the CLI

```bash
./mvnw -pl tnra-cli-app -am package -DskipTests
```

### 2. Provision a new group

```bash
java -jar tnra-cli-app/target/tnra-cli.jar provision tnra1 \
  --domain dev.dogeared.dev \
  --admin-email jerry@seinfeld.com \
  --admin-first-name Jerry \
  --admin-last-name Seinfeld
```

Save the temporary admin password shown in the output. The admin must change it on first login.

This generates 7 files in `provision/tnra1/`:
- `docker-compose.yml` — app container on the shared `tnra-shared` network (the billing token, if any,
  is referenced as `${TNRA_BILLING_API_TOKEN_<GROUP>}` from the host's `.env` — the CLI prints the line)
- `tnra1-realm.json` — Keycloak realm with client, roles, and admin user
- `tnra1.conf` — Nginx subdomain routing
- `init-db.sql` — MySQL database and user creation
- `seed-admin.sql` — inserts the admin user into the app database
- `.env` — group-specific credentials (host-accessible URLs for IDE dev)
- `INSTRUCTIONS.md` — step-by-step guide (covers IDE dev, Docker, and production)

### 3. Set up local DNS

Add to `/etc/hosts`:

```
127.0.0.1 auth.dev.dogeared.dev tnra1.dev.dogeared.dev
```

### 4. Set up local HTTPS (one-time)

```bash
mkdir -p nginx/.cert
mkcert -install
mkcert -cert-file nginx/.cert/cert.pem -key-file nginx/.cert/key.pem \
  localhost auth.dev.dogeared.dev "*.dev.dogeared.dev"
```

### 5. Initialize the database

```bash
docker compose exec -T mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> \
  < provision/tnra1/init-db.sql
```

### 6. Import the Keycloak realm

```bash
cp provision/tnra1/tnra1-realm.json keycloak/
docker compose restart keycloak
```

Verify at `https://auth.dev.dogeared.dev/admin` (admin/admin): the `tnra1` realm should appear
with the admin user pre-created.

### 7. Start the group's container

```bash
# Build the production JAR first (if not already built — the group container
# bakes tnra-app/target/*.jar via its build context of ./tnra-app)
./mvnw -pl tnra-app -am clean package -DskipTests -Pproduction

# Layer the group onto the dev compose so it joins the tnra-shared network and reads
# TNRA_ENCRYPTION_MASTER_KEY from the repo-root .env. Naming the service starts only it
# (the shared infra from the Prerequisites stays up).
docker compose -f docker-compose.yml -f provision/tnra1/docker-compose.yml up --build -d tnra-tnra1
```

### 8. Seed the admin user

After the app has started and Flyway has created the schema:

```bash
docker compose exec -T mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> \
  < provision/tnra1/seed-admin.sql
```

To load demo data instead (optional, requires a SQL dump file):

```bash
docker compose exec -T mysql mysql -utnra_tnra1 -p<password_from_provision/.env> tnra_tnra1 \
  < /path/to/demo-data.sql
```

### 9. Copy the Nginx config and reload

```bash
cp provision/tnra1/tnra1.conf nginx/sites/
docker compose restart proxy
```

### 10. Access the group

Visit `https://tnra1.dev.dogeared.dev` and log in with the admin credentials from step 2.
The admin will be prompted to change their password on first login.

Each group has fully isolated data, users, and authentication.

### Removing a group

To fully remove a provisioned group (e.g., `tnra1`):

```bash
# 1. Stop and remove just the group's container (leaves the shared infra running)
docker compose -f docker-compose.yml -f provision/tnra1/docker-compose.yml rm -sf tnra-tnra1

# 2. Remove the Nginx config and reload
rm nginx/sites/tnra1.conf
docker compose restart proxy

# 3. Drop the database and user
docker compose exec mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> -e "
  DROP DATABASE IF EXISTS tnra_tnra1;
  DROP USER IF EXISTS 'tnra_tnra1'@'%';
  FLUSH PRIVILEGES;
"

# 4. Remove the Keycloak realm
# Option A: Delete via admin console at https://auth.dev.dogeared.dev/admin
#   Select the realm > Realm Settings > Action dropdown > Delete
# Option B: Reset Keycloak data (re-imports all realm JSONs from scratch)
rm keycloak/tnra1-realm.json
docker compose down keycloak
docker volume rm tnra_keycloak-data
docker compose up keycloak -d
# Note: simply restarting Keycloak does NOT remove existing realms.
# Keycloak only imports realm JSONs on first start with an empty database.

# 5. Remove provisioned files and registry entry
rm -rf provision/tnra1
# Edit groups.json and remove the tnra1 entry
```

### Group registry

Provisioned groups are tracked in `groups.json` (gitignored). Copy `groups.json.example`
to `groups.json` before first use. The CLI auto-assigns ports (8081, 8082,
etc.) and prevents duplicate group names.

## Bumping the Version

The version lives in every `pom.xml` and in `VERSION` (which CI reads to tag `main`), and CI fails if
they disagree. Bump them all at once:

```bash
./bump-version.sh patch      # 10.6.6 -> 10.6.7   (or: minor, major, or an explicit 10.8.2)
```

It runs the versions-maven-plugin (`versions:set … -DprocessAllModules`) for the poms, writes `VERSION`,
and starts a `## [x.y.z] - <today>` section at the top of `CHANGELOG.md` with a TODO line for you to
replace (`--no-changelog` skips that). It doesn't commit — review the diff, write the entry, commit.

## Running Tests

```bash
./mvnw clean test
```

JaCoCo coverage reports are generated per module under `<module>/target/site/jacoco/`
(e.g. `tnra-app/target/site/jacoco/` for the main app).

Tests use H2 with `ddl-auto: create-drop` (Flyway disabled). Flyway migrations are
MySQL-specific SQL and are not run during tests.

## Auth Configuration

Login navigation is configurable through `AuthNavigationService`:

- `tnra.auth.login-path`: explicit login URL path (e.g., `/oauth2/authorization/google`)
- `tnra.auth.login-registration-id`: provider registration id (fallback)

Resolution order: `login-path` > `/oauth2/authorization/{login-registration-id}` >
`/oauth2/authorization/keycloak`

## Slack Integration

TNRA uses [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks) — a one-way
push mechanism that requires no bot token, no OAuth flow, and no Slack app review. When
enabled, a notification fires each time a member finishes a post:

```
[First Last] finished a post | Started: 2024-01-15 08:30 | Finished: 2024-01-15 09:15 | View: https://…
```

No post content is included. Only the activity metadata (who, when, link) leaves the app.

> **Compared to the v4.x integration:** The old implementation required a Slack bot access
> token (`xoxb-…`), a broadcast channel ID, and a separate Slack token — these had to be
> managed as app credentials and involved OAuth scopes. The webhook approach used here is
> simpler: one URL, tied to a channel at creation time, no credentials stored beyond the URL
> itself (which is encrypted at rest with AES-256-GCM).

### Setting up a Slack webhook

1. Go to [https://api.slack.com/apps](https://api.slack.com/apps) and click **Create New App → From scratch**.
2. Name the app (e.g., `TNRA Notifications`) and select your workspace. Click **Create App**.
3. In the left sidebar under **Features**, click **Incoming Webhooks**.
4. Toggle **Activate Incoming Webhooks** to **On**.
5. Click **Add New Webhook to Workspace**.
6. Select the channel where post-completion notifications should appear (e.g., `#accountability`). Click **Allow**.
7. Copy the generated webhook URL — it follows the pattern:
   ```
   https://hooks.slack.com/services/<workspace-id>/<channel-id>/<token>
   ```
   Keep this URL private; anyone with it can post to your channel.

### Configuring TNRA

1. Log in as a group admin and open the **Admin** panel.
2. Click the **Integrations** tab.
3. Paste the webhook URL into the **Slack Webhook URL** field.
4. Check **Slack notifications enabled**.
5. Click **Save**.

Notifications fire asynchronously and never block the member's post submission. Errors
(e.g., invalid URL, network failure) are logged server-side and do not surface to the user.

To disable notifications without losing the URL, uncheck **Slack notifications enabled** and save.

## Email

Three different things send or receive mail, and they are set up independently.

### Outbound: the apps send through Mailgun

The landing site's "Request Access" notification and the billing service's "free period ending"
reminders (10.6.4+) both go out through Mailgun, configured by `MAILGUN_KEY_PRIVATE` and `MAILGUN_URL`
in `.env`. `MAILGUN_URL` must be the full messages endpoint of an **active, verified** sending domain:

```
MAILGUN_URL=https://api.mailgun.net/v3/mail.tnra.app/messages
```

Production sends through `mail.tnra.app`. A Mailgun domain that has been suspended (e.g. for account
inactivity) answers `403 Domain … is not allowed to send` — the app logs
`Failed to send Mailgun notification` with that text, and the request itself is still saved. Reproduce
outside the app with:

```bash
curl -s -i -u "api:$MAILGUN_KEY_PRIVATE" "$MAILGUN_URL" \
  -F from=tnra-landing@tnra.app -F to=you@example.com -F subject=check -F text=check
```

The sender is `TNRA_NOTIFY_FROM` (default `tnra-landing@tnra.app`). Keep it on, or under, the Mailgun
sending domain so SPF/DKIM align with the From domain; a mismatch lands the mail in spam.

### Inbound: the founder address

Notifications go to `TNRA_FOUNDER_EMAIL` (default `micah@tnra.app`). That mailbox is not hosted
anywhere: **Cloudflare Email Routing** on the `tnra.app` zone forwards `micah@tnra.app` to a Gmail
account (Email → Email Routing → destination address + custom address; Cloudflare adds the apex MX and
SPF records itself). Replies are sent *as* `micah@tnra.app` from Gmail via Settings → Accounts and
Import → "Send mail as", using an SMTP relay with its own credentials (Mailgun `smtp.mailgun.org:587`
with an SMTP login on `mail.tnra.app`, or Mailjet `in-v3.mailjet.com:587`) — not `smtp.gmail.com`, which
would sign the mail for gmail.com and fail alignment for `tnra.app`. Only one SPF TXT may exist on the
apex, so a second relay's `include:` is merged into Cloudflare's record rather than added beside it.

### Keycloak password-reset mail

Separate again: the per-group Keycloak realm's SMTP block (`--smtp-*` options to `tnra-cli provision`)
is what sends "forgot password" mail. It is optional and off unless those options were given.

## Deploying: `.env` gotchas

Docker Compose resolves every `${VAR}` in `docker-compose.production.yml` and the group overlays from
the `.env` **in the directory it runs in** — nowhere else. Two ways this goes wrong:

- **Wrong directory.** Running `docker compose` from the build checkout instead of the deploy
  directory (`~/tnra` in production) interpolates every variable to blank: the landing container then
  connects to MySQL as `root` with no password, `TNRA_ENCRYPTION_MASTER_KEY` is empty, and the group's
  billing token is blank. Build anywhere; run compose from the directory that holds `.env`.
- **Shell variables win over `.env`.** `set -a; source .env; set +a` (handy for `curl`ing the admin or
  Mailgun APIs) exports every value into that shell session. From then on compose uses the *shell's*
  value, so a later edit to `.env` is silently ignored in that terminal — `docker exec <container> env`
  shows the old value even after `--force-recreate`. Open a fresh shell, or `unset` the variable, before
  bringing a container up.

Check what a container actually received with `docker exec tnra-landing env | grep TNRA_`.

## Database Migrations (Flyway)

Migration files: `tnra-app/src/main/resources/db/migration/`

| Migration | Description |
|-----------|-------------|
| V1 | Baseline schema (users, posts with embedded stats) |
| V2 | Make slack columns nullable, drop PQ token columns |
| V3 | Configurable stats (stat_definition + post_stat_value) |
| V4 | Notification preferences |
| V5 | Personal stats and email unique constraint |
| V6 | Convert post.state from TINYINT to VARCHAR |
| V7 | AES-256-GCM encryption schema: create encryption_keys table, widen sensitive columns to TEXT |
| V8 | (Java) Encrypt existing post and stat data in-place |
| V9 | Widen stat_definition.emoji to TEXT |
| V10 | (Java) Encrypt existing emoji values in-place |
| V11 | Group settings (Slack webhook URL encrypted, enabled flag) |

Flyway runs automatically on startup. `baseline-on-migrate: true` handles pre-Flyway databases.
Hibernate `ddl-auto: validate` ensures schema matches entities without modifying the database.

**Adding a new migration:**

SQL migration: create `tnra-app/src/main/resources/db/migration/V{N}__{description}.sql`

Java migration (for data transforms, e.g. encryption): create `tnra-app/src/main/java/com/afitnerd/tnra/db/migration/V{N}__{ClassName}.java` extending `BaseJavaMigration` with `@Component`. Must be in the `com.afitnerd.tnra` package tree so Spring detects it. If the migration needs the encryption master key, inject it via `@Value("${tnra.encryption.master-key}")` in the constructor — Spring resolves it at context load time and fails fast if the property is absent.

Migrations run automatically on startup. Test SQL migrations against MySQL (not H2 — syntax differs).

See `MIGRATION-V3-STATS.md` for the detailed V3 configurable stats migration plan.

## Project Structure

As of v9.0.0 this is a Maven multi-module project. The root `pom.xml` is an
aggregator; three sibling modules build independently. From the repo root,
`./mvnw clean test` runs every module's tests; `./mvnw -pl tnra-app -am ...`
scopes commands to a single module.

```
tnra/
├── pom.xml                          # Aggregator (packaging=pom, modules=tnra-app, tnra-cli-app, tnra-landing-app)
├── tnra-app/                        # Main app — Spring Boot + Vaadin
│   ├── pom.xml
│   ├── Dockerfile                   # App server container (eclipse-temurin:21, non-root user)
│   ├── docker-entrypoint.sh
│   └── src/main/
│       ├── java/com/afitnerd/tnra/
│       │   ├── vaadin/              # Vaadin Flow views
│       │   ├── model/               # JPA entities
│       │   ├── service/             # Business logic
│       │   └── repository/          # Spring Data JPA repositories
│       └── resources/
│           ├── application.yml      # Spring Boot configuration
│           └── db/migration/        # Flyway SQL migrations
├── tnra-cli-app/                    # Provisioning CLI (picocli + gson, standalone)
│   ├── pom.xml
│   └── src/main/java/com/afitnerd/tnra/cli/   # TnraCli, ProvisionCommand, GroupRegistry, etc.
├── tnra-landing-app/                # Public landing page at tnra.app
│   ├── pom.xml
│   ├── Dockerfile
│   ├── Dockerfile.dev
│   └── src/main/
├── keycloak/                        # Realm config (tnra-realm.json) and custom themes
├── nginx/
│   ├── templates/                   # Default Nginx config template
│   ├── sites/                       # Per-group Nginx server blocks (generated by CLI)
│   └── .cert/                       # SSL certificates
├── docker-compose.yml               # Dev infrastructure only (MySQL, Keycloak, Nginx) — apps run from the IDE
├── docker-compose.production.yml    # Shared production infra (mysql, keycloak, tnra-landing, cloudflared)
├── groups.json.example              # Template for group registry (copy to groups.json)
└── uploads/                         # Profile image storage (created at runtime)
```
