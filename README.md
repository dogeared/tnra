[![Build](https://github.com/dogeared/tnra/actions/workflows/tests.yml/badge.svg)](https://github.com/dogeared/tnra/actions/workflows/tests.yml)
[![Test Coverage](.github/badges/jacoco.svg)](https://github.com/dogeared/tnra/actions/workflows/tests.yml)

# TNRA — Structured Accountability for Groups

TNRA is a structured accountability group app. Members track daily/weekly/monthly/yearly
cadences: daily call chains, weekly posts (intro, kryptonite, commitments, personal/family/work
best & worst, stats), monthly meetings, and yearly retreats.

Built with Spring Boot 3.5, Vaadin Flow 24.9, Java 21, and MySQL 8.

## Post View Modes

|                     | Completed View                                                                  | In Progress View                                                        |
|---------------------|---------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| In Progress Post    | - switch to in progress button - completed posts dropdown - pagination controls | - switch to completed button - post started date & time - finish button |
| No In Progress Post | - start new post button - completed posts dropdown - pagination controls        | XXXX                                                                    |

## Running Locally

### Option 1: H2 in-memory (quick start, no Docker)

Tests use H2 automatically. For local dev with H2:

```bash
# Set required env vars
export OKTA_OAUTH2_ISSUER=https://your-issuer.example.com/oauth2/default
export OKTA_OAUTH2_CLIENT_ID=your-client-id

# Run with H2 (data resets on restart)
./mvnw spring-boot:run
```

The app listens on port 8080. H2 uses `ddl-auto: create-drop` and Flyway is disabled.

### Option 2: MySQL via Docker (persistent data, matches production)

Start MySQL:

```bash
docker compose up mysql -d
```

This starts MySQL on port 3307 (mapped from container's 3306). The database `tnra` is
created automatically.

Run the app against MySQL:

```bash
./mvnw spring-boot:run
```

The default `application.yml` points to `localhost:3307/tnra`. On first run against a
fresh MySQL database, Flyway will run all migrations (V1 baseline + V2 schema cleanup).
On subsequent runs, Flyway only runs new migrations.

### Option 3: Full stack via Docker Compose (app + MySQL + Nginx)

```bash
# Copy and fill in environment variables
cp .env.template .env
# Edit .env with your values

# Build the jar
./mvnw clean package -DskipTests -Pproduction

# Start everything
docker compose up --build -d
```

The app is available at https://localhost:443 (via Nginx proxy).

## Database Migrations (Flyway)

This project uses [Flyway](https://flywaydb.org/) for versioned database migrations.

**Migration files:** `src/main/resources/db/migration/`

| Migration | Description |
|-----------|-------------|
| V1__baseline.sql | Initial schema (users, post, go_to_guy_set, go_to_guy_pair) |
| V2__remove_slack_pq_columns.sql | Make slack columns nullable, drop PQ token columns |

**How it works:**
- On startup, Flyway checks the `flyway_schema_history` table to see which migrations have run.
- New migrations are applied automatically. Already-applied migrations are skipped.
- `baseline-on-migrate: true` means existing databases (from the pre-Flyway era) are
  automatically baselined at V1 — Flyway won't try to re-create existing tables.
- Hibernate `ddl-auto: validate` verifies the schema matches the entity model but never
  modifies the database. All schema changes go through Flyway migrations.

**Adding a new migration:**
1. Create `src/main/resources/db/migration/V{N}__{description}.sql`
2. Write the SQL (use `ALTER TABLE` for schema changes, `INSERT/UPDATE` for data migrations)
3. Test locally against MySQL (not H2 — migration SQL may differ)
4. The migration runs automatically on next app startup

**Tests:** Flyway is disabled in tests (`spring.flyway.enabled=false`). Tests use H2 with
`ddl-auto: create-drop` for speed. The Flyway migrations are MySQL-specific SQL.

## Auth Configuration

Login navigation is configurable through `AuthNavigationService`.

- `tnra.auth.login-path`: explicit login URL path (e.g., `/oauth2/authorization/google`)
- `tnra.auth.login-registration-id`: provider registration id used when `login-path` is not set

Resolution: `login-path` > `/oauth2/authorization/{login-registration-id}` > `/oauth2/authorization/okta`

```bash
export TNRA_AUTH_LOGIN_REGISTRATION_ID=google
```

## Running Tests

```bash
./mvnw clean test
```

234 tests covering controllers, services, presenters, models, and Vaadin views.
JaCoCo coverage reports are generated in `target/site/jacoco/`.

## Production Deployment (Vultr VPS)

### Current Setup

The app runs on a Vultr VPS with Docker Compose: app container + MySQL container + Nginx
proxy with SSL termination.

```bash
# Check service status
systemctl status tnra.service

# View logs
docker compose logs -f server
```

### Deploying a New Release

After merging a PR to main:

```bash
# On the VPS
cd ~/tnra
git pull origin main

# Rebuild and restart
./mvnw clean package -DskipTests -Pproduction
docker compose up --build -d

# Flyway migrations run automatically on startup
# Verify the app started correctly:
docker compose logs -f server
# Look for: "Started TnraApplication" and "Successfully applied N migrations"
```

### Branch-by-Branch Deployment Notes

This project is being productionized in 5 sequential branches. Each branch has specific
deployment considerations:

**Branch 1: Flyway + Remove Slack/SMS/PQ** (this branch)
- First deploy with Flyway. On the existing production database:
  - Flyway will baseline at V1 (existing schema, no changes)
  - V2 runs: makes `slack_username`/`slack_user_id` nullable, drops `pq_access_token`/`pq_refresh_token`
- Slack, SMS, and PQ features are removed. If any external integration was hitting
  the old `/api/v1/post`, `/api/v1/pq`, or other Slack endpoints — they will now
  return 403 (authentication required).
- The `tnra.notify.schedule` property remains but the scheduled SMS notification
  is removed. No scheduled tasks will run.
- **Rollback:** Revert to previous Docker image. V2 column drops are irreversible
  (PQ tokens are lost) but slack columns are only made nullable, not dropped.

**Branch 2: Configurable Stats** (upcoming)
- V3 migration: creates `stat_definition` and `post_stat_value` tables, migrates
  existing embedded stats data.
- **Before deploying:** Back up the database. The stats data migration is irreversible.
- After deploy: verify old posts still display correctly with migrated stats.

**Branch 3: Keycloak + Auth + Activity-Only Email** (upcoming)
- Requires a running Keycloak instance before deploy.
- Replaces Okta OAuth2. Update `OKTA_OAUTH2_*` env vars → Keycloak equivalents.
- Email templates change from full-content to activity-only.
- **Before deploying:** Set up Keycloak, create realm, configure Google social login.

**Branch 4: Provisioning + Infrastructure** (upcoming)
- Introduces the provisioning CLI and per-group Docker Compose files.
- Changes the deployment model from single-instance to shared-infra multi-group.
- **Deployment is a migration**, not just an update — the entire infrastructure changes.

**Branch 5: Landing Page + Encryption** (upcoming)
- Adds a public landing page route (unauthenticated).
- Enables database encryption at rest.
- **Before deploying:** Configure DB encryption (MySQL TDE or PostgreSQL equivalent).

## SSL Certificate Renewal

### On local machine

```
certbot certonly \
  --config-dir ~/letsencrypt/config \
  --work-dir ~/letsencrypt/work \
  --logs-dir ~/letsencrypt/logs \
  --dns-cloudflare --dns-cloudflare-credentials ~/cloudflare-creds.ini \
  -d tnra.afitnerd.com
scp \
  /Users/micahsilverman/letsencrypt/config/live/tnra.afitnerd.com/fullchain.pem \
  /Users/micahsilverman/letsencrypt/config/live/tnra.afitnerd.com/privkey.pem \
  tnra@108.61.192.65:~/
```

### On Vultr

```
mv privkey.pem tnra/nginx/.cert/key.pem
mv fullchain.pem tnra/nginx/.cert/cert.pem
```

## Helpful Commands

Dump the database:

```bash
mysqldump -h <host> -u<username> -p --skip-column-statistics --no-tablespaces <database> > ~/tnra.sql
```
