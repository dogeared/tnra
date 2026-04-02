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
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=<choose_a_password>
VAADIN_PRODUCTIONMODE=false
```

Keycloak defaults work out of the box for local dev (`tnra-app` / `tnra-app-secret` /
`http://localhost:8180/realms/tnra`). Only set `KEYCLOAK_*` variables if you customize Keycloak.

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
# Start MySQL and Keycloak
docker compose up mysql keycloak -d

# Wait for MySQL to be healthy
docker compose exec mysql mysqladmin ping -h localhost --wait=30

# Run the app
./mvnw spring-boot:run
```

MySQL is available at `localhost:3307` (mapped from container port 3306). The `tnra` database
is created automatically by Docker. Flyway runs all migrations on first start.

**Optional: create a non-root database user**

```bash
docker compose exec mysql mysql -uroot -p<your_password> -e "
  CREATE USER 'tnra' IDENTIFIED BY '<user_password>';
  GRANT ALL PRIVILEGES ON tnra.* TO 'tnra'@'%';
  FLUSH PRIVILEGES;
"
```

Then update `application.yml` datasource credentials to match.

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

Pre-configured test accounts: `admin@tnra.local` / `admin` (admin role),
`member@tnra.local` / `member`.

### 6. Access the application

- **Options A/B:** `http://localhost:8080`
- **Option C:** `https://localhost:443`

## Running Multiple Groups Locally

Each TNRA group runs as its own container with its own database and Keycloak realm. The
`tnra-cli` tool generates all the config files you need to provision a new group.

### Prerequisites

- The base infrastructure running (`docker compose up mysql keycloak -d`)
- [mkcert](https://github.com/FiloSottile/mkcert) installed for local HTTPS
- Java 21 (to run the CLI)

### 1. Build the CLI

```bash
cd cli && mvn package -DskipTests && cd ..
```

### 2. Provision a new group

```bash
java -jar cli/target/tnra-cli.jar provision recovery-guys --domain afitnerd.local
```

This generates 6 files in `provision/recovery-guys/`:
- `docker-compose.yml` — app container on the shared network
- `recovery-guys-realm.json` — Keycloak realm with client and roles
- `recovery-guys.conf` — Nginx subdomain routing
- `init-db.sql` — MySQL database and user creation
- `.env` — group-specific credentials
- `INSTRUCTIONS.md` — step-by-step guide

### 3. Set up local DNS

Add to `/etc/hosts`:

```
127.0.0.1 recovery-guys.afitnerd.local
```

### 4. Set up local HTTPS (one-time)

```bash
mkcert -install
mkcert -cert-file nginx/.cert/cert.pem -key-file nginx/.cert/key.pem \
  localhost tnra.afitnerd.local "*.afitnerd.local"
```

### 5. Initialize the database

```bash
docker compose exec -T mysql mysql -uroot -p<password> \
  < provision/recovery-guys/init-db.sql
```

### 6. Import the Keycloak realm

```bash
cp provision/recovery-guys/recovery-guys-realm.json keycloak/
docker compose restart keycloak
```

Verify at `http://localhost:8180/admin`: the `recovery-guys` realm should appear.

### 7. Start the group's container

```bash
docker compose -f provision/recovery-guys/docker-compose.yml up --build -d
```

### 8. Copy the Nginx config and reload

```bash
cp provision/recovery-guys/recovery-guys.conf nginx/sites/
docker compose restart proxy
```

### 9. Create an admin user

1. Go to `http://localhost:8180/admin`
2. Switch to the `recovery-guys` realm
3. Users > Add user (set email, name)
4. Credentials > Set password
5. Role Mappings > Assign `admin` and `member`

### 10. Access the group

Visit `https://recovery-guys.afitnerd.local` and log in.

The original group continues to work at its existing URL. Each group has fully isolated
data, users, and authentication.

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
