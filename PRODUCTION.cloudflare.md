# Production Deployment Guide — Cloudflare Tunnels

This guide covers deploying TNRA using [Cloudflare Tunnels](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/) instead of a public-IP VPS with Nginx and certbot.

**Key differences from the VPS setup (`PRODUCTION.vps.md`):**

| Concern | VPS setup | Cloudflare Tunnels |
|---|---|---|
| TLS certificates | certbot + Let's Encrypt | Cloudflare edge (automatic) |
| Inbound port 443 | Required | Not needed |
| DNS records | Manual A records | Auto-created CNAME by Cloudflare |
| Nginx | SSL termination + routing | Not needed |
| New group setup | cert + nginx site file | One dashboard click |
| DDoS / WAF | Self-managed | Cloudflare built-in |

## Prerequisites

- A Cloudflare account with your domain **using Cloudflare as the authoritative DNS** (required for tunnel CNAME auto-creation).
- A VPS with SSH access. A public IP is not required for HTTPS traffic; only SSH needs to be reachable.
- Cloudflare **Zero Trust** is available on the free plan. Open the Cloudflare dashboard → **Zero Trust** to activate it (one-time setup, no cost).

## How it Works

```
Browser
  │  HTTPS (TLS terminated at Cloudflare edge)
  ▼
Cloudflare Edge
  │  Encrypted tunnel (outbound from VPS, no open ports)
  ▼
cloudflared  (Docker container on VPS, tnra-shared network)
  │  HTTP (plain, internal Docker network)
  ├──► tnra-group-a:8080
  ├──► tnra-group-b:8080
  └──► keycloak:8080        (auth.your-domain.com route)
```

`cloudflared` maintains a persistent outbound connection to Cloudflare's edge. No inbound ports beyond SSH are required.

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

### 5. Configure environment variables

```bash
cp .env.template .env
```

Edit `.env` and set all required values:

```bash
SPRING_DATASOURCE_USERNAME=tnra      # updated after MySQL first boot (step 6)
SPRING_DATASOURCE_PASSWORD=<strong_app_password>
VAADIN_PRODUCTIONMODE=true
KEYCLOAK_CLIENT_ID=tnra-app
KEYCLOAK_CLIENT_SECRET=<generate_a_real_secret>   # rotated after Keycloak first boot (step 7)
KEYCLOAK_ISSUER_URI=https://auth.your-domain.com/realms/tnra
```

Generate and set the encryption master key:

```bash
TNRA_ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32)
```

**Set this once before first startup and never change it.** The master key wraps the
per-group Data Encryption Key stored in the `encryption_keys` table. Rotating the master
key without re-encrypting the DEK first will make all post content permanently unreadable.
Store the value in a password manager or secrets vault.

Set Mailgun credentials if email notifications are enabled:

```bash
MAILGUN_KEY_PRIVATE=<your_key>
MAILGUN_KEY_PUBLIC=<your_key>
MAILGUN_URL=https://api.mailgun.net/v3/your-domain.com/messages
```

### 6. Set MySQL passwords

Generate a strong root password and set it in `.env` **before the first `docker compose up`**:

```bash
# In .env
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32)
```

Write that value down — you'll need it to create the app user below.

On first container start, `mysql/init-local-user.sql` auto-creates a `tnra` database user
with a hardcoded development password (`123456aA$`). Change it immediately after MySQL comes up:

```bash
# Start MySQL only first
docker compose up mysql -d
docker compose exec mysql mysqladmin ping -h localhost --wait=30

# Change the app user's password
docker compose exec mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "
  ALTER USER 'tnra'@'%' IDENTIFIED BY '<strong_app_password>';
  FLUSH PRIVILEGES;
"
```

Then update `.env` so the app connects as the `tnra` user, not root:

```bash
SPRING_DATASOURCE_USERNAME=tnra
SPRING_DATASOURCE_PASSWORD=<strong_app_password>
```

### 7. Set Keycloak admin credentials

Generate a strong Keycloak admin password and set it in `.env` **before the first
`docker compose up`**:

```bash
# In .env
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -base64 32)
```

`docker-compose.yml` reads these via `${KEYCLOAK_ADMIN:-admin}` and
`${KEYCLOAK_ADMIN_PASSWORD:-admin}` — the fallback `admin`/`admin` defaults are only safe
for local development.

After first boot, rotate the Keycloak client secret (see step 4 in
[Create a Cloudflare Tunnel](#4-configure-routes-and-rotate-the-keycloak-client-secret)
below — the admin console is reachable via the tunnel without an SSH tunnel).

## Create a Cloudflare Tunnel

### 1. Create the tunnel

1. Log in to the [Cloudflare dashboard](https://dash.cloudflare.com).
2. Go to **Zero Trust** → **Networks** → **Tunnels**.
3. Click **Add a tunnel** → choose **Cloudflared** → name it (e.g., `tnra-production`).
4. On the next screen, select **Docker** as the connector environment and copy the full
   `docker run` command — you only need the `--token <TOKEN>` value from it.
5. Add it to `.env`:

```bash
CLOUDFLARE_TUNNEL_TOKEN=<token_from_cloudflare_dashboard>
```

Click **Next** — you'll configure routes in step 3.

### 2. Add the cloudflared service to docker-compose

Add the following service to `docker-compose.yml`. It joins the existing `tnra-shared`
network so it can reach all app containers and Keycloak by hostname:

```yaml
cloudflared:
  image: cloudflare/cloudflared:latest
  command: tunnel --no-autoupdate run --token ${CLOUDFLARE_TUNNEL_TOKEN}
  restart: unless-stopped
  networks:
    - tnra-shared
```

The `proxy` (Nginx) service is not needed for routing or SSL with Cloudflare Tunnels.
You can remove it from `docker-compose.yml` or leave it commented out.

### 3. Configure public hostname routes

Back in the Cloudflare dashboard → **Zero Trust** → **Networks** → **Tunnels** → your tunnel
→ **Public Hostnames** tab, add routes for each service:

| Subdomain | Domain | Service |
|---|---|---|
| (blank) | tnra.app | `http://tnra-main:8080` |
| auth | tnra.app | `http://keycloak:8080` |

Replace `tnra.app` with your actual domain and `tnra-main` with the container name of your
app (as set in the group's `docker-compose.<group>.yml`).

Cloudflare automatically creates CNAME DNS records for each route. No manual DNS setup required.

### 4. Configure routes and rotate the Keycloak client secret

Because the Keycloak admin console is reachable at `https://auth.your-domain.com/admin`
through the tunnel, no SSH tunnel is needed:

1. Open `https://auth.your-domain.com/admin` and log in with your `KEYCLOAK_ADMIN` credentials.
2. Switch to the `tnra` realm → **Clients** → `tnra-app` → **Credentials** tab.
3. Click **Regenerate** next to Client secret and copy the new value.
4. Update `.env`: `KEYCLOAK_CLIENT_SECRET=<new_secret>`
5. Restart the app container: `docker compose restart server`

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

```bash
cd ~/tnra

# Build the app
./mvnw clean package -DskipTests -Pproduction

# Start all services (mysql, keycloak, cloudflared, app containers)
docker compose up --build -d

# Verify the app started
docker compose logs -f server
# Look for: "Started TnraApplication" and Flyway migration messages
```

### Subsequent deployments

```bash
cd ~/tnra
git pull origin main

./mvnw clean package -DskipTests -Pproduction
docker compose up --build -d

docker compose logs -f server
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

The service runs `tnra.start.sh`, which sources `.env` and runs `docker-compose up --build --detach`.

```bash
# Check status
systemctl status tnra.service

# View logs
docker compose logs -f server

# Stop
./tnra.stop.sh  # or: systemctl stop tnra.service
```

## Firewall Configuration

With Cloudflare Tunnels, HTTPS traffic arrives via the outbound tunnel — **port 443 does
not need to be open**. Only SSH is required:

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw enable
```

MySQL (3307) and Keycloak (8180) are bound to `127.0.0.1` in `docker-compose.yml` and are
not exposed externally. The `cloudflared` container reaches Keycloak via the internal
`tnra-shared` Docker network, not via a host port.

## Hardening MySQL

Root password and app-user password setup are covered in step 6 of
[Initial Server Setup](#initial-server-setup). The items below cover ongoing hardening.

### MySQL data persistence

MySQL data is stored in the `mysql-db` Docker volume. Back up regularly:

```bash
docker compose exec mysql mysqldump -uroot -p<root_password> \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql
```

### Remote access

The MySQL container is bound to `127.0.0.1:3307` and is not reachable from outside the
host. Access the MySQL shell via:

```bash
docker compose exec mysql mysql -utnra -p<app_password> tnra
```

## Hardening Keycloak

Admin credential setup and client secret rotation are covered in steps 7 and
[Create a Cloudflare Tunnel](#create-a-cloudflare-tunnel) above.

### Restrict Keycloak admin access with Cloudflare Access

The Keycloak admin console is exposed at `https://auth.your-domain.com/admin` through the
tunnel. Restrict access to trusted IPs or email addresses using Cloudflare Access:

1. Cloudflare dashboard → **Zero Trust** → **Access** → **Applications** → **Add an application**.
2. Choose **Self-hosted**, set the application domain to `auth.your-domain.com`.
3. Add a policy: allow only your IP range or require email OTP for your address.

This prevents anyone without Cloudflare Access approval from reaching the admin console,
even though the URL is technically public.

### Run in production mode

The default `docker-compose.yml` uses `start-dev` for simplicity. For production, switch
the Keycloak command to `start`, which enforces HTTPS and optimizes caching:

```yaml
keycloak:
  command: >
    start
    --import-realm
    --hostname=https://auth.your-domain.com
    --proxy-headers=xforwarded
    --http-enabled=true
```

### Keycloak data persistence

Keycloak data is stored in the `keycloak-data` Docker volume. The realm configuration is
auto-imported from `keycloak/tnra-realm.json` on first start. Back up the volume regularly:

```bash
docker run --rm -v tnra_keycloak-data:/data -v ~/backups:/backup \
  alpine tar czf /backup/keycloak-data-$(date +%Y%m%d).tar.gz -C /data .
```

## Provisioning New Groups

Each TNRA group runs as its own Docker container with its own MySQL database and Keycloak
realm. With Cloudflare Tunnels, adding a group requires no SSL certificate and no Nginx
config — just a new tunnel route and a new container.

### Architecture

```
                    ┌──────────────────────────────────────┐
                    │           Shared VPS                  │
                    │                                       │
                    │  cloudflared ──► tnra-group-a:8080   │◄── group-a.tnra.app
                    │            └──► tnra-group-b:8080   │◄── group-b.tnra.app
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

All containers share the `tnra-shared` Docker network.

### Step 1: Build the CLI (on your local machine)

```bash
cd cli && mvn package -DskipTests && cd ..
```

### Step 2: Provision

```bash
java -jar cli/target/tnra-cli.jar provision <group-name> --domain tnra.app
```

Replace `tnra.app` with your production domain. This generates config files in
`provision/<group-name>/`. The generated `nginx.conf` and SSL-related steps in the
`instructions.md` output are not needed with Cloudflare Tunnels — skip them.

### Step 3: Add a Cloudflare tunnel route

In the Cloudflare dashboard → **Zero Trust** → **Networks** → **Tunnels** → your tunnel
→ **Public Hostnames** → **Add a public hostname**:

- **Subdomain**: `<group-name>`
- **Domain**: `tnra.app`
- **Service**: `http://tnra-<group-name>:8080`

Cloudflare automatically creates the CNAME DNS record. No DNS or SSL setup required.

### Step 4: Copy files to VPS

```bash
scp -r provision/<group-name>/ tnra@<VPS_IP>:~/
```

### Step 5: Initialize the database

```bash
docker compose exec -T mysql mysql -uroot -p<root_password> \
  < ~/<group-name>/init-db.sql
```

### Step 6: Import the Keycloak realm

```bash
cp ~/<group-name>/<group-name>-realm.json ~/tnra/keycloak/
docker compose restart keycloak
```

Verify via `https://auth.your-domain.com/admin`:
- Realm dropdown shows `<group-name>`
- Client `<group-name>-app` is configured with correct redirect URIs

### Step 7: Deploy the group's app container

```bash
cd ~/tnra
cp ~/<group-name>/docker-compose.yml ~/tnra/docker-compose.<group-name>.yml
docker compose -f docker-compose.<group-name>.yml up --build -d
```

Watch logs:

```bash
docker compose -f docker-compose.<group-name>.yml logs -f
```

Look for: `Started TnraApplication` and Flyway migration messages.

### Step 8: Create the first admin user

1. Open `https://auth.your-domain.com/admin`
2. Switch to the `<group-name>` realm
3. **Users** → **Add user** (set email, first name, last name)
4. **Credentials** → Set password (disable "Temporary")
5. **Role Mappings** → Assign `admin` and `member` roles

### Step 9: Verify

Visit `https://<group-name>.tnra.app` and log in with the admin user.

### Managing multiple groups

```bash
# View all running group containers
docker ps --filter "name=tnra-"

# View logs for a specific group
docker compose -f docker-compose.<group-name>.yml logs -f

# Restart a specific group
docker compose -f docker-compose.<group-name>.yml restart

# Back up a specific group's database
docker compose exec mysql mysqldump -uroot -p<root_password> \
  --skip-column-statistics --no-tablespaces <db_name> \
  > ~/backups/<group-name>-$(date +%Y%m%d).sql
```

### Group registry

All provisioned groups are tracked in `groups.json` (gitignored, not checked in). Copy
`groups.json.example` to `groups.json` before first use. The CLI auto-assigns ports and
prevents duplicate group names. Keep this file backed up on the VPS.

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
./mvnw spring-boot:run
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

./mvnw clean package -DskipTests -Pproduction
docker compose up --build -d

docker compose logs -f server
# Verify: "Successfully applied N migrations" and "Started TnraApplication"
```

### Rollback

If migration fails: restore from backup and redeploy the previous version.

```bash
docker compose exec -T mysql mysql -uroot -p<password> tnra < ~/tnra-backup-*.sql
# Redeploy previous git tag/commit
```

The V3 stats migration drops the old embedded columns — there is no partial rollback.
Always back up first.

## Helpful Commands

```bash
# View running containers
docker compose ps

# View app logs
docker compose logs -f server

# View cloudflared tunnel status
docker compose logs cloudflared
docker compose exec cloudflared cloudflared tunnel info

# Database shell
docker compose exec mysql mysql -utnra -p<app_password> tnra

# Dump the database
docker compose exec mysql mysqldump -uroot -p<root_password> \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql

# Restart a single service
docker compose restart server

# Restart the tunnel (e.g. after rotating the token)
docker compose restart cloudflared

# Rebuild and restart everything
docker compose up --build -d
```
