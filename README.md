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
- Java 21 (for running outside Docker)
- Maven (or use the included `./mvnw` wrapper)

## Local Development Setup

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

### 3. Choose a run option

#### Option A: H2 in-memory (quick start, no MySQL needed)

Tests use H2 automatically. For local dev with H2 (data resets on restart):

```bash
# Start Keycloak (pre-configured realm auto-imports on first start)
docker compose up keycloak -d

# Run with H2
./mvnw spring-boot:run
```

H2 uses `ddl-auto: create-drop` and Flyway is disabled.

#### Option B: MySQL via Docker (persistent data, matches production)

```bash
# Start MySQL and Keycloak (first time)
docker compose up mysql keycloak -d

# Or, restart existing containers from a previous run
docker compose start mysql keycloak

# Wait for MySQL to be healthy
docker compose exec mysql mysqladmin ping -h localhost --wait=30

# Run the app
./mvnw spring-boot:run
```

MySQL is available at `localhost:3307` (mapped from container port 3306). The `tnra` database
and a `tnra` user (matching `application.yml` defaults) are created automatically on first
container start. Flyway runs all migrations on first app start.

> **Note:** If you already have a MySQL Docker volume, the init script won't re-run.
> Reset with: `docker compose down -v && docker compose up mysql keycloak -d`

#### Option C: Full stack via Docker Compose (app + MySQL + Keycloak + Nginx)

```bash
# Build the production JAR
./mvnw clean package -DskipTests -Pproduction

# Start everything
docker compose up --build -d
```

The app is available at `https://localhost:443` via Nginx proxy (requires SSL certs, see below).

### 4. SSL Certificates for Local Nginx

Only needed for Option C. Place certs in `nginx/.cert/`:

```bash
mkdir -p nginx/.cert

# Option 1: mkcert (trusted local certs)
mkcert -install
mkcert -cert-file nginx/.cert/cert.pem -key-file nginx/.cert/key.pem localhost

# Option 2: Self-signed (browser will show warning)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/.cert/key.pem -out nginx/.cert/cert.pem \
  -subj "/CN=localhost"
```

### 5. Create a test user in Keycloak

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

### 6. Access the application

- **Options A/B:** `http://localhost:8080`
- **Option C:** `https://localhost:443`

## Running Multiple Groups Locally

Each TNRA group runs as its own container with its own database and Keycloak realm. The
`tnra-cli` tool generates all the config files you need to provision a new group.

### Prerequisites

- The base infrastructure running (`docker compose up mysql keycloak -d` for first run, or `docker compose start mysql keycloak` to restart existing containers)
- [mkcert](https://github.com/FiloSottile/mkcert) installed for local HTTPS
- Java 21 (to run the CLI)

### 1. Build the CLI

```bash
cd cli && mvn package -DskipTests && cd ..
```

### 2. Provision a new group

```bash
java -jar cli/target/tnra-cli.jar provision tnra1 \
  --domain dev.dogeared.dev \
  --admin-email jerry@seinfeld.com \
  --admin-first-name Jerry \
  --admin-last-name Seinfeld
```

Save the temporary admin password shown in the output. The admin must change it on first login.

This generates 7 files in `provision/tnra1/`:
- `docker-compose.yml` — app container on the shared network
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
# Build the production JAR first (if not already built)
./mvnw clean package -DskipTests -Pproduction

docker compose -f provision/tnra1/docker-compose.yml up --build -d
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
# 1. Stop the group's container
docker compose -f provision/tnra1/docker-compose.yml down

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
| V6 | Convert post.state from TINYINT to VARCHAR |
| V7 | AES-256-GCM encryption schema: create encryption_keys table, widen sensitive columns to TEXT |
| V8 | (Java) Encrypt existing post and stat data in-place |
| V9 | Widen stat_definition.emoji to TEXT |
| V10 | (Java) Encrypt existing emoji values in-place |

Flyway runs automatically on startup. `baseline-on-migrate: true` handles pre-Flyway databases.
Hibernate `ddl-auto: validate` ensures schema matches entities without modifying the database.

**Adding a new migration:**

SQL migration: create `src/main/resources/db/migration/V{N}__{description}.sql`

Java migration (for data transforms, e.g. encryption): create `src/main/java/com/afitnerd/tnra/db/migration/V{N}__{ClassName}.java` extending `BaseJavaMigration` with `@Component`. Must be in the `com.afitnerd.tnra` package tree so Spring detects it.

Migrations run automatically on startup. Test SQL migrations against MySQL (not H2 — syntax differs).

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
├── keycloak/            # Realm config (tnra-realm.json) and custom themes
├── nginx/
│   ├── templates/       # Default Nginx config template
│   ├── sites/           # Per-group Nginx server blocks (generated by CLI)
│   └── .cert/           # SSL certificates
├── docker-compose.yml   # Base infrastructure (app, MySQL, Keycloak, Nginx)
├── groups.json.example  # Template for group registry (copy to groups.json)
├── Dockerfile           # App server container (eclipse-temurin:21, non-root user)
└── uploads/             # Profile image storage (created at runtime)
```
