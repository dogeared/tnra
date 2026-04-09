[![Build](https://github.com/dogeared/tnra/actions/workflows/tests.yml/badge.svg)](https://github.com/dogeared/tnra/actions/workflows/tests.yml)
[![Test Coverage](.github/badges/jacoco.svg)](https://github.com/dogeared/tnra/actions/workflows/tests.yml)

# TNRA - Structured Accountability for Groups

TNRA is a structured accountability group app. Members track daily/weekly/monthly/yearly
cadences: daily call chains, weekly posts (intro, kryptonite, commitments, personal/family/work
best & worst, stats), monthly meetings, and yearly retreats.

## Architecture

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Backend        | Spring Boot 3.5, Java 21           |
| Frontend       | Vaadin Flow 24.9 (server-side Java UI) |
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
- Java 21
- Maven (or use the included `./mvnw` wrapper)
- [mkcert](https://github.com/FiloSottile/mkcert) for local HTTPS

## How It Works

TNRA is multi-tenant. Each group gets its own database, Keycloak realm, and subdomain.
The `tnra-cli` tool provisions everything for a new group. The root `docker-compose.yml`
runs shared infrastructure only (MySQL, Keycloak, Nginx). App containers are per-group.

This means local development, local multi-tenant testing, and production all work the
same way: start infrastructure, provision a group with the CLI, run the app.

## Quick Start (H2, no infrastructure needed)

For quick iteration without MySQL or multi-tenant setup:

```bash
git clone https://github.com/dogeared/tnra
cd tnra

# Start Keycloak (pre-configured realm auto-imports on first start)
docker compose up keycloak -d

# Run with H2 in-memory database (data resets on restart)
./mvnw spring-boot:run
```

H2 uses `ddl-auto: create-drop` and Flyway is disabled. Access at `http://localhost:8080`.

Pre-configured test accounts: `admin@tnra.local` / `admin` (admin role),
`member@tnra.local` / `member`.

## Local Development Setup (MySQL)

This is the standard development workflow. It uses the same provisioning as production.

### 1. Start infrastructure

```bash
git clone https://github.com/dogeared/tnra
cd tnra

# Create .env with your MySQL root password
cp .env.template .env
# Edit .env and set MYSQL_ROOT_PASSWORD

# Start infrastructure (first time)
docker compose up -d

# Or, restart existing containers from a previous run
docker compose start
```

### 2. Set up local HTTPS (one-time)

```bash
mkdir -p nginx/.cert
mkcert -install
mkcert -cert-file nginx/.cert/cert.pem -key-file nginx/.cert/key.pem \
  localhost "*.afitnerd.local"
```

### 3. Build the CLI and provision a group

```bash
cd cli && mvn package -DskipTests && cd ..

# Provision a group (generates DB, Keycloak realm, Nginx config, etc.)
java -jar cli/target/tnra-cli.jar provision my-group --domain afitnerd.local
```

This generates 6 files in `provision/my-group/`:
- `docker-compose.yml` — app container on the shared network
- `my-group-realm.json` — Keycloak realm with client and roles
- `my-group.conf` — Nginx subdomain routing
- `init-db.sql` — MySQL database and user creation
- `.env` — group-specific credentials
- `INSTRUCTIONS.md` — step-by-step guide

### 4. Initialize the group's database

```bash
docker compose exec -T mysql mysql -uroot -p<your_MYSQL_ROOT_PASSWORD> \
  < provision/my-group/init-db.sql
```

### 5. Import the Keycloak realm

```bash
cp provision/my-group/my-group-realm.json keycloak/
docker compose restart keycloak
```

Verify at `http://localhost:8180/admin` (admin/admin): the `my-group` realm should appear.

### 6. Set up local DNS

Add to `/etc/hosts`:

```
127.0.0.1 my-group.afitnerd.local
```

### 7. Copy the Nginx config and reload

```bash
cp provision/my-group/my-group.conf nginx/sites/
docker compose restart proxy
```

### 8. Run the app from your IDE

Configure your IDE or command line to use the group's environment variables from
`provision/my-group/.env`. The key variables are:

- `SPRING_DATASOURCE_URL` — points to the group's database
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` — group's DB credentials
- `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` — group's Keycloak client
- `KEYCLOAK_ISSUER_URI` — group's Keycloak realm

Or run from the command line:

```bash
# Source the group's env vars and run
set -a && source provision/my-group/.env && set +a
./mvnw spring-boot:run
```

### 9. Create an admin user

1. Go to `http://localhost:8180/admin`
2. Switch to the `my-group` realm
3. Users > Add user (set email, name)
4. Credentials > Set password
5. Role Mappings > Assign `admin` and `member`

### 10. Access the app

Visit `https://my-group.afitnerd.local` and log in.

## Running Multiple Groups Locally

The same provisioning flow supports multiple groups. Each group is fully isolated
with its own database, Keycloak realm, and subdomain.

To add another group, repeat steps 3-7 with a different group name:

```bash
java -jar cli/target/tnra-cli.jar provision another-group --domain afitnerd.local

docker compose exec -T mysql mysql -uroot -p<your_MYSQL_ROOT_PASSWORD> \
  < provision/another-group/init-db.sql

cp provision/another-group/another-group-realm.json keycloak/
docker compose restart keycloak

# Add to /etc/hosts: 127.0.0.1 another-group.afitnerd.local

cp provision/another-group/another-group.conf nginx/sites/
docker compose restart proxy
```

To run a group as a Docker container (instead of from your IDE):

```bash
# Build the production JAR first
./mvnw clean package -DskipTests -Pproduction

# Start the group's container
docker compose -f provision/another-group/docker-compose.yml up --build -d
```

Visit `https://another-group.afitnerd.local` and log in.

### Group registry

Provisioned groups are tracked in `groups.json` (gitignored). Copy `groups.json.example`
to `groups.json` before first use. The CLI auto-assigns ports (8081, 8082,
etc.) and prevents duplicate group names.

## Running Tests

```bash
./mvnw clean test
```

JaCoCo coverage reports are generated in `target/site/jacoco/`.

Tests use H2 with `ddl-auto: create-drop` (Flyway disabled). Flyway migrations are
MySQL-specific SQL and are not run during tests.

## Auth Configuration

Login navigation is configurable through `AuthNavigationService`:

- `tnra.auth.login-path`: explicit login URL path (e.g., `/oauth2/authorization/google`)
- `tnra.auth.login-registration-id`: provider registration id (fallback)

Resolution order: `login-path` > `/oauth2/authorization/{login-registration-id}` >
`/oauth2/authorization/keycloak`

## Database Migrations (Flyway)

Migration files: `src/main/resources/db/migration/`

| Migration | Description |
|-----------|-------------|
| V1 | Baseline schema (users, posts with embedded stats) |
| V2 | Make slack columns nullable, drop PQ token columns |
| V3 | Configurable stats (stat_definition + post_stat_value) |
| V4 | Notification preferences |
| V5 | Personal stats and email unique constraint |

Flyway runs automatically on startup. `baseline-on-migrate: true` handles pre-Flyway databases.
Hibernate `ddl-auto: validate` ensures schema matches entities without modifying the database.

**Adding a new migration:**

1. Create `src/main/resources/db/migration/V{N}__{description}.sql`
2. Write the SQL (`ALTER TABLE` for schema, `INSERT/UPDATE` for data)
3. Test locally against MySQL (not H2 -- migration SQL may differ)
4. Migration runs automatically on next app startup

See `MIGRATION-V3-STATS.md` for the detailed V3 configurable stats migration plan.

## Project Structure

```
tnra/
├── src/main/java/com/afitnerd/tnra/
│   ├── vaadin/          # Vaadin Flow views
│   ├── model/           # JPA entities
│   ├── service/         # Business logic
│   └── repository/      # Spring Data JPA repositories
├── src/main/resources/
│   ├── application.yml  # Spring Boot configuration
│   └── db/migration/    # Flyway SQL migrations
├── cli/                 # Provisioning CLI (separate Maven project, not in app build)
│   ├── pom.xml
│   └── src/main/java/   # TnraCli, ProvisionCommand, GroupRegistry, etc.
├── keycloak/            # Realm config and custom themes
├── nginx/
│   ├── templates/       # Default Nginx config template
│   ├── sites/           # Per-group Nginx server blocks (generated by CLI)
│   └── .cert/           # SSL certificates
├── provision/           # Per-group configs (generated by CLI, gitignored)
├── docker-compose.yml   # Infrastructure only (MySQL, Keycloak, Nginx)
├── groups.json.example  # Template for group registry (copy to groups.json)
├── Dockerfile           # App server container (eclipse-temurin:21, non-root user)
└── uploads/             # Profile image storage (created at runtime)
```
