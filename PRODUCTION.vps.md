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
5. Update the group's env file with `KEYCLOAK_CLIENT_SECRET=<new_secret>`.
6. Restart the group container.

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

```bash
cd ~/tnra

# Install Maven wrapper dependencies and build
./mvnw clean package -DskipTests -Pproduction

# Start all services
docker compose up --build -d
```

### Subsequent deployments

```bash
cd ~/tnra
git pull origin main

./mvnw clean package -DskipTests -Pproduction
docker compose up --build -d

# Verify startup
docker compose logs -f server
# Look for: "Started TnraApplication" and any Flyway migration messages
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

## Hardening Keycloak

Admin credential setup and client secret rotation are covered in step 7 of
[Initial Server Setup](#initial-server-setup). The items below cover ongoing hardening.

### Run in production mode

The default `docker-compose.yml` uses `start-dev` for simplicity. For production, switch
the Keycloak command to `start`, which enforces HTTPS and optimizes caching:

```yaml
keycloak:
  command: >
    start
    --import-realm
    --hostname=https://your-domain.com
    --proxy-headers=xforwarded
    --http-enabled=true
```

### Restrict Keycloak admin access

Keycloak is bound to `127.0.0.1:8180` in `docker-compose.yml` and is not reachable from
outside the host. Always access the admin console via SSH tunnel:

```bash
ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>
# Then open http://localhost:8180/admin in your browser
```

### Keycloak data persistence

Keycloak data is stored in the `keycloak-data` Docker volume. Realms are imported via the
admin UI during group provisioning (not on startup). Back up the volume regularly:

```bash
docker run --rm -v tnra_keycloak-data:/data -v ~/backups:/backup \
  alpine tar czf /backup/keycloak-data-$(date +%Y%m%d).tar.gz -C /data .
```

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

All containers share the `tnra-production-shared` Docker network. Each group's container connects to
MySQL and Keycloak using internal Docker hostnames (`mysql`, `keycloak`).

### Step 1: Build the CLI (on your local machine)

```bash
cd cli && mvn package -DskipTests && cd ..
```

### Step 2: Provision

```bash
java -jar cli/target/tnra-cli.jar provision <group-name> --domain tnra.app
```

Replace `tnra.app` with your production domain. This generates 6 files in
`provision/<group-name>/`.

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

### Step 5: Copy files to VPS

```bash
scp -r provision/<group-name>/ tnra@<VPS_IP>:~/
```

### Step 6: Set the encryption master key in the group's `.env`

The CLI generates a placeholder `TNRA_ENCRYPTION_MASTER_KEY=` in the group's `.env`. Fill it in
with the value from your production `.env` before starting the app container — Flyway migrations
V8 and V10 require it at startup and the app will fail to start if it is missing.

```bash
# On the VPS, copy the value from the production .env:
grep TNRA_ENCRYPTION_MASTER_KEY ~/tnra/.env

# Then edit the group .env:
nano ~/<group-name>/.env
# Set: TNRA_ENCRYPTION_MASTER_KEY=<value-from-above>
```

### Step 7: Initialize the database

```bash
docker compose -f docker-compose.production.yml exec -T mysql mysql -uroot -p<root_password> \
  < ~/<group-name>/init-db.sql
```

### Step 8: Import the Keycloak realm

Use the Keycloak admin UI to import the realm (no restart required):

1. Open an SSH tunnel: `ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>`
2. Open `http://localhost:8180/admin` and log in with your Keycloak admin credentials.
3. Click the realm dropdown (top-left) → **Create realm**.
4. Click **Browse**, select `provision/<group-name>/<group-name>-realm.json`.
5. Click **Create**.

Verify:
- Realm dropdown shows `<group-name>`
- Client `<group-name>-app` is configured with correct redirect URIs

### Step 9: Deploy the group's app container

```bash
cd ~/tnra
cp ~/<group-name>/docker-compose.yml ~/tnra/docker-compose.<group-name>.yml
docker compose -f docker-compose.production.yml -f docker-compose.<group-name>.yml up --build -d
```

Watch logs:
```bash
docker compose -f docker-compose.<group-name>.yml logs -f
```

Look for: `Started TnraApplication` and Flyway migration messages.

### Step 10: Configure Nginx

```bash
cp ~/<group-name>/<group-name>.conf ~/tnra/nginx/sites/
docker compose -f docker-compose.production.yml restart proxy
```

### Step 11: Create the first admin user

1. SSH tunnel: `ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>`
2. Open `http://localhost:8180/admin`
3. Switch to the `<group-name>` realm
4. Users > Add user (set email, first name, last name)
5. Credentials > Set password (disable "Temporary")
6. Role Mappings > Assign `admin` and `member` roles

### Step 12: Verify

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
(name: "tnra", port: 8080). After adding the `tnra-production-shared` network to `docker-compose.yml`
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
docker compose -f docker-compose.<group-name>.yml logs -f

# Restart a group app
docker compose -f docker-compose.<group-name>.yml restart
```
