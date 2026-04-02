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
├── keycloak/            # Realm config (tnra-realm.json) and custom themes
├── nginx/               # Nginx config templates and SSL certs
├── docker-compose.yml   # Docker Compose service definitions
├── Dockerfile           # App server container (eclipse-temurin:21, non-root user)
└── uploads/             # Profile image storage (created at runtime)
```
