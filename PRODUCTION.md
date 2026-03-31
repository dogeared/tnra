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

### 5. Configure environment variables

```bash
cp .env.template .env
```

Edit `.env` and set all required values:

```bash
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=<strong_password>
VAADIN_PRODUCTIONMODE=true
KEYCLOAK_CLIENT_ID=tnra-app
KEYCLOAK_CLIENT_SECRET=<generate_a_real_secret>
KEYCLOAK_ISSUER_URI=https://your-domain.com/realms/tnra
```

Set Mailgun credentials if email notifications are enabled:

```bash
MAILGUN_KEY_PRIVATE=<your_key>
MAILGUN_KEY_PUBLIC=<your_key>
MAILGUN_URL=https://api.mailgun.net/v3/your-domain.com/messages
```

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

### Use a strong root password

Set a strong password in `.env` for `SPRING_DATASOURCE_PASSWORD`. This is used as
`MYSQL_ROOT_PASSWORD` in the MySQL container.

### Create a dedicated application user

Don't use root for the application. After MySQL starts:

```bash
docker compose exec mysql mysql -uroot -p<root_password> -e "
  CREATE USER 'tnra'@'%' IDENTIFIED BY '<app_password>';
  GRANT SELECT, INSERT, UPDATE, DELETE ON tnra.* TO 'tnra'@'%';
  FLUSH PRIVILEGES;
"
```

Then update `.env`:

```bash
SPRING_DATASOURCE_USERNAME=tnra
SPRING_DATASOURCE_PASSWORD=<app_password>
```

Note: you'll also need to update `docker-compose.yml` to set `MYSQL_ROOT_PASSWORD` separately
from `SPRING_DATASOURCE_PASSWORD`, since the app user should not be root.

### MySQL data persistence

MySQL data is stored in the `mysql-db` Docker volume. Back up regularly:

```bash
docker compose exec mysql mysqldump -uroot -p<password> \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql
```

### Disable remote root login

The MySQL container is already bound to `127.0.0.1:3307` in `docker-compose.yml`, so it's not
accessible from outside the host. Keep it this way.

## Hardening Keycloak

### Change admin credentials

The default admin credentials (`admin`/`admin`) must be changed in production.

Update `docker-compose.yml` or use environment variables:

```bash
KEYCLOAK_ADMIN=<new_admin_username>
KEYCLOAK_ADMIN_PASSWORD=<strong_admin_password>
```

In `docker-compose.yml`, change the keycloak service:

```yaml
keycloak:
  environment:
    KC_BOOTSTRAP_ADMIN_USERNAME: ${KEYCLOAK_ADMIN:-admin}
    KC_BOOTSTRAP_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
```

### Run in production mode

Change the Keycloak command from `start-dev` to `start` with proper settings:

```yaml
keycloak:
  command: >
    start
    --import-realm
    --hostname=https://your-domain.com
    --proxy-headers=xforwarded
    --http-enabled=true
```

In production mode, Keycloak enforces HTTPS and optimizes caching.

### Generate a real client secret

1. Log into the Keycloak admin console
2. Go to the `tnra` realm > Clients > `tnra-app`
3. Under Credentials, regenerate the client secret
4. Update `KEYCLOAK_CLIENT_SECRET` in `.env`

### Restrict Keycloak admin access

Keycloak is bound to `127.0.0.1:8180` in `docker-compose.yml`. Access the admin console
via SSH tunnel:

```bash
ssh -L 8180:127.0.0.1:8180 tnra@<VPS_IP>
# Then open http://localhost:8180/admin in your browser
```

### Keycloak data persistence

Keycloak data is stored in the `keycloak-data` Docker volume. The realm configuration is
auto-imported from `keycloak/tnra-realm.json` on first start. Back up the volume:

```bash
docker run --rm -v tnra_keycloak-data:/data -v ~/backups:/backup \
  alpine tar czf /backup/keycloak-data-$(date +%Y%m%d).tar.gz -C /data .
```

## Database Migration from V1

If you have an existing V1 database (pre-Flyway, with hardcoded embedded stats), follow this
process to migrate to the current schema.

### Overview

The migration path is: V1 (baseline) -> V2 (remove slack/PQ columns) -> V3 (configurable stats)
-> V4 (notification preferences) -> V5 (personal stats + email unique).

Flyway handles all of this automatically on startup with `baseline-on-migrate: true`.

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
-- Should show V1 (baseline), V2, V3, V4, V5 all with success=1

-- Verify configurable stats migrated
SELECT COUNT(*) FROM stat_definition;   -- should be 7
SELECT COUNT(*) FROM post_stat_value;   -- should match your non-null stat count

-- Verify old embedded columns are gone
DESCRIBE post;
-- Must NOT have: exercise, gtg, meditate, meetings, pray, _read, sponsor
```

See `MIGRATION-V3-STATS.md` for the detailed V3 migration plan with step-by-step verification.

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
# View running containers
docker compose ps

# View app logs
docker compose logs -f server

# Database shell
docker compose exec mysql mysql -uroot -p<password> tnra

# Dump the database
docker compose exec mysql mysqldump -uroot -p<password> \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql

# Restart a single service
docker compose restart server

# Rebuild and restart everything
docker compose up --build -d
```
