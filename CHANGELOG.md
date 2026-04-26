# Changelog

All notable changes to TNRA are documented in this file.

## [8.1.8] - 2026-04-26

### Fixed
- **`ClientRegistrationRepository` not found on production startup.** The per-group `docker-compose.yml.tmpl` used `env_file: .env`, which resolved to the shared infrastructure `.env` (with empty `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_ISSUER_URI`) when run from the tnra deployment directory. Spring Boot therefore saw empty OAuth2 registration properties and skipped auto-configuring `ClientRegistrationRepository`. Fixed by changing the env_file reference to `{{GROUP_NAME}}/.env` so each group's credentials live in a named subdirectory (`~/tnra/<group-name>/.env`) and can't be confused with the shared `.env`.
- **MySQL URL in per-group `.env` used host-mapped port 3307.** The app runs on the `tnra-production-shared` Docker network and connects to MySQL via the Docker-internal hostname `mysql:3306`. The localhost/port-mapped URL was only needed for direct admin access from the host and is not used by the app container. Template updated to use `mysql:3306` directly; the now-redundant `SPRING_DATASOURCE_URL` environment override in `docker-compose.yml.tmpl` was removed.
- **PRODUCTION guides updated** with consolidated Step 4/5 that copies the group's `.env`, injects the encryption master key, and stages both the env and docker-compose in the tnra deployment directory before launching.

## [8.1.7] - 2026-04-26

### Fixed
- **Production app startup failure (`BeanCreationException` for V8/V10 Flyway migrations).** CLI-generated per-group `.env` was missing `TNRA_ENCRYPTION_MASTER_KEY`, causing the Spring beans for `V8__EncryptExistingData` and `V10__EncryptEmojiData` to fail at startup with an unresolved `@Value`. The CLI's `env.tmpl` now includes a placeholder `TNRA_ENCRYPTION_MASTER_KEY=` with a warning comment; the operator must copy the value from the production `.env` before starting the app container.
- **Per-group app containers unable to reach MySQL/Keycloak.** CLI's `docker-compose.yml.tmpl` referenced the old external network name `tnra-shared`; updated to `tnra-production-shared` to match `docker-compose.production.yml`.
- **PRODUCTION guides updated.** Both `PRODUCTION.vps.md` and `PRODUCTION.cloudflare.md` now reference `tnra-production-shared` throughout and include an explicit new step (Step 6) instructing operators to fill in `TNRA_ENCRYPTION_MASTER_KEY` in the group's `.env` before deploying the app container.

## [8.1.6] - 2026-04-26
- **`docker-compose.production.yml` now has production mysql exposed port on 3308. changed shared network name to: `tnra-production-shared`. these changes are so as not to conflict with existing production config as the transition to the new architecture is made.

## [8.1.5] - 2026-04-26

### Fixed
- **`bootstrap.sh` MySQL password now masked.** The MySQL root password prompt was using `read -r` (which echoes input) instead of `prompt_secret()`. All other secret prompts were already correctly hidden.
- **Production containers renamed to avoid dev conflicts.** `docker-compose.production.yml` now uses `container_name: mysql-prod` and `container_name: keycloak-prod`, and sets `name: tnra-production` at the project level so Docker Compose scopes volumes as `tnra-production_mysql-db` and `tnra-production_keycloak-data`. This prevents name and volume collisions when dev containers are running on the same machine simultaneously. Service names (`mysql`, `keycloak`) are unchanged so per-group app containers continue to resolve them correctly on the `tnra-shared` network.

## [8.1.4] - 2026-04-26

### Added
- **`bootstrap.sh`** — interactive production setup script. Prompts for domain, MySQL root password, Keycloak admin credentials, encryption master key, and optional Cloudflare Tunnel token (auto-generating any you leave blank). Writes `.env`, starts MySQL and Keycloak (plus `cloudflared` when a token is provided), waits for MySQL health, and prints all generated credentials with next-step instructions.
- **`docker-compose.production.yml`** — minimal shared-services compose for production. MySQL has no default database and no init-script mount (all databases are provisioned by `tnra-cli`). Keycloak runs `start-dev` without `--import-realm` (realms are imported via the admin UI). `cloudflared` is gated behind `profiles: [cloudflare]`. Both services use `restart: unless-stopped`.

### Changed
- **Keycloak realm import flow.** Realm import during group provisioning now uses the Keycloak admin UI (realm dropdown → Create realm → Browse → select JSON → Create) instead of copying the file and restarting Keycloak.
- **`PRODUCTION.vps.md` and `PRODUCTION.cloudflare.md` updated.** Initial setup steps 5–7 (env vars, MySQL passwords, Keycloak credentials) replaced with a single `./bootstrap.sh` step. All `docker compose` commands in provisioning and helpful-commands sections updated to use `-f docker-compose.production.yml`.
- **`KEYCLOAK_HOSTNAME` added to `.env.template`** with a note to set it to `https://auth.<domain>` in production.

## [8.1.3] - 2026-04-26

### Changed
- **`PRODUCTION.md` renamed to `PRODUCTION.vps.md`.** Clarifies that the existing guide is specific to a VPS + Nginx + certbot deployment.
- **New `PRODUCTION.cloudflare.md` guide.** Complete standalone deployment guide for Cloudflare Tunnels: no SSL certificates, no open inbound port 443, no Nginx required. Covers tunnel creation, `cloudflared` Docker service, per-group public hostname routing via the Cloudflare dashboard, Cloudflare Access for Keycloak admin hardening, and simplified group provisioning (one dashboard click instead of cert + nginx site file).
- **MySQL initial setup steps added to both production guides.** Explicit steps to generate a strong `MYSQL_ROOT_PASSWORD` before first boot, and to immediately rotate the hardcoded dev password (`123456aA$`) on the `tnra` app user created by `init-local-user.sql`.
- **Keycloak initial setup steps added to both production guides.** `docker-compose.yml` now reads admin credentials from `${KEYCLOAK_ADMIN:-admin}` and `${KEYCLOAK_ADMIN_PASSWORD:-admin}` instead of hardcoded values. Guides include instructions to set a strong `KEYCLOAK_ADMIN_PASSWORD` before first boot and rotate the client secret after first boot.
- **`TNRA_ENCRYPTION_MASTER_KEY` added to `.env.template`** with generation command (`openssl rand -base64 32`) and a warning about key rotation.
- **`KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `CLOUDFLARE_TUNNEL_TOKEN` added to `.env.template`.**
- **`PRODUCTION.vps.md` updated** to document all feature changes since v7.3.0: encryption at rest (V7–V10 migration path), Slack notifications, deep links, member deactivation, and Copy Link.

## [8.1.2] - 2026-04-26

### Added
- **Copy Link button in completed post view.** When a completed post is selected from the date dropdown, a "Copy Link" button (copy icon) appears to its right. Clicking it writes the post's encrypted deep-link URL to the clipboard via `navigator.clipboard` and shows a "Link copied to clipboard!" notification. The button is disabled until a post is selected, absent in the in-progress view, and pre-enabled when a post is loaded via deep link.
- **559 unit tests.** 10 new `PostViewTest` cases cover: button absent in in-progress view, present-but-disabled with no selection, enabled on selection, disabled on deselection, clipboard JS verified on click, pre-enabled for deep-linked posts, and four `buildPostUrl` variants (valid post, null post, null ID, trailing-slash base URL).

## [8.1.1] - 2026-04-26

### Added
- **Slack Part 1 — activity webhook notifications.** When a post is finished, a Slack incoming-webhook message is sent asynchronously via a dedicated `slackTaskExecutor` thread pool. The notification includes the user's display name, started/finished timestamps, and a tokenised deep-link URL. Sending is gated by two `GroupSettings` flags: `slackEnabled` and `slackWebhookUrl`; either being false/blank silently skips the notification.
- **`SlackNotificationService` / `SlackNotificationServiceImpl`.** New service encapsulates message construction (`buildMessage`) and delivery (`doPost`). The base URL is injected via `tnra.app.base-url` and has trailing slashes stripped at construction time.
- **549 unit tests.** New `SlackNotificationServiceImplTest` covers 12 scenarios: constructor URL normalisation, message formatting (full name, first-name-only, email fallback, "Someone" fallback), null start/finish dates, null post ID, mrkdwn injection escaping, skip-path early exits (disabled, null URL, blank URL), `doPost` SSRF rejection, and IOException logging.

### Security
- **SSRF guard on Slack webhook.** `doPost()` rejects any URL that does not begin with `https://hooks.slack.com/`, preventing a compromised `GroupSettings` record from redirecting notifications to an attacker-controlled server.
- **HTTPS enforcement.** Plain-`http://` Slack URLs are rejected by the same prefix check, ensuring credentials are never sent in cleartext.
- **Slack mrkdwn injection prevention.** `escapeSlack()` replaces `&`, `<`, and `>` with HTML entities in all user-controlled strings before they are embedded in the notification payload.
- **Encoded tokens in log messages.** `SlackNotificationServiceImpl` log lines now reference `postTokenService.encode(post.getId())` instead of the raw database primary key, preventing sequential ID exposure in application logs.
- **Null-user deep-link guard.** `PostView.initializeUser()` now falls through to the default in-progress post lookup when a deep-linked post has no associated user, rather than returning early with no user-visible feedback — consistent with all other error paths.

### Changed
- **`PostTokenServiceImpl` constant extraction.** The `"ENC:"` literal used as a ciphertext prefix is now a named constant `ENC_PREFIX`, eliminating the magic string.

## [8.1.0] - 2026-04-25

### Added
- **Encrypted post deep links.** Raw database IDs are no longer exposed in post URLs. A new `PostTokenService` encrypts each `Long` post ID using the existing AES-256-GCM DEK and encodes the result as URL-safe base64 (no `+`, `/`, or `=` padding). `PostView` now accepts `HasUrlParameter<String>` and decodes the token server-side before fetching the post; invalid or tampered tokens fall through to the default view with no information leaked. Both Slack notifications (`SlackNotificationServiceImpl`) and email notifications (`ActivityNotificationRenderer`) now generate tokenised URLs.
- **`PostTokenService` / `PostTokenServiceImpl`.** Standalone encode/decode service. Encode: `encrypt(id.toString())` → strip `ENC:` prefix → convert standard base64 to URL-safe base64url (no padding). Decode: reverse the base64url mapping → restore padding → prepend `ENC:` → `decrypt()` → `Long.parseLong()`. Any exception in this chain throws `IllegalArgumentException`.
- **542 unit tests.** 7 new `PostTokenServiceImplTest` tests cover padding strips, character substitution, round-trip, URL-safety, and two error paths. `PostViewTest` updated with 30 two-arg constructor calls and a new `testDeepLinkWithInvalidTokenFallsThroughToDefault` test.

### Security
- Sequential integer IDs no longer appear in Slack messages, email notifications, or browser URLs. Authenticated users cannot enumerate posts by incrementing a URL parameter.

## [8.0.3] - 2026-04-25

### Changed
- **Test coverage raised to 95.2%.** Extracted package-private methods from Vaadin grid column lambdas in `AdminView` and `ProfileView` so they can be unit tested directly. Moved `userService.getCurrentUser()` call in `MainView` into the `showAuthenticatedView()` try/catch block, fixing the error-fallback path. Added `MockedStatic<VaadinService>` + `MockedStatic<RouteConfiguration>` setup to `MainLayoutTest` so `RouterLink.setRoute()` resolves without a full Spring context.
- **512 unit tests** now cover all production code paths added across `AdminView`, `MainView`, `ProfileView`, `MainLayout`, and `PostServiceImpl`.

## [8.0.2] - 2026-04-24

### Changed
- **Spring `@Value` injection for Flyway migration master key.** `V8__EncryptExistingData` and `V10__EncryptEmojiData` now receive the master key via `@Value("${tnra.encryption.master-key}")` constructor injection instead of `System.getenv()`. Spring resolves the key at context load time, so a missing `TNRA_MASTER_KEY` env var fails fast on startup rather than mid-migration.

## [8.0.1] - 2026-04-24

### Added
- **Encrypt `stat_definition.emoji` column.** Extends AES-256-GCM encryption to the `emoji` field on `StatDefinition` (covers both global and personal stats via single-table inheritance). Flyway V9 widens the column to `TEXT`; V10 encrypts existing emoji values in-place. No information about a group's tracking categories is now visible in a compromised database.

## [8.0.0] - 2026-04-24

### Added
- **AES-256-GCM column encryption.** All sensitive post and stat data is now encrypted at rest:
  - 9 post text fields: `widwytk`, `kryptonite`, `what_and_when`, and all 6 best/worst category fields
  - 2 stat definition fields: `name`, `label`
  - 1 post stat value field: `stat_value`
- **Per-application DEK.** A 256-bit Data Encryption Key (DEK) is generated on first startup, encrypted with a master key, and stored in the new `encryption_keys` table. The master key is supplied via `TNRA_MASTER_KEY` env var (falls back to a dev default when absent).
- **Transparent JPA encryption.** `EncryptedStringConverter` and `EncryptedIntegerConverter` (`AttributeConverter`) apply AES-256-GCM automatically on read/write — application code is unchanged.
- **Flyway V7 schema migration.** Creates `encryption_keys` table, widens `stat_definition.name`/`label` and `post_stat_value.stat_value` to `TEXT`, drops the now-invalid unique index on `stat_definition.name`.
- **Flyway V8 data migration.** Java migration encrypts all existing plaintext values in-place (idempotent — skips rows already prefixed with `ENC:`).
- **In-memory name uniqueness checks.** Replaced DB-level `WHERE name = ?` queries (broken with random-IV ciphertext) with decrypt-and-compare streams in `AdminView` and `ProfileView`.

## [7.5.4] - 2026-04-23

- updated docker-compose.yml and default nginx template to remove old server config. Makes local dev from IDE easier

## [7.5.3] - 2026-04-21

### Added
- 71 new tests raising JaCoCo instruction coverage from 83.9% to 90.7%.
  - PostViewTest (15): pagination (next, previous, first, last, goToPage), finishPost success, updateFinishButtonState (4 states), generatePostLabel (4 variants), savePostChanges, page size selector.
  - AdminViewTest (8): member grid deactivate/reactivate/self branches, name column rendering, GTG set creation, refresh grid delegation.
  - AdminViewDialogTest (13): invite dialog (valid, invalid, empty, duplicate email), add stat dialog (success, duplicate, empty, null emoji), add pair dialog (render, valid pair, invalid pair, caller filtering).
  - MainLayoutUtilityTest (21): createTab, createDrawer (unauthenticated, authenticated, admin), openLogoutDialog, executeDirectLogout (CSRF paths, fallbacks), toggleTheme (both directions, persistence, null user, exception), applyTheme, onAttach.
  - Model tests (6): PostStatValue constructors/getters, PersonalStatDefinition constructors.
  - Utility tests (6): PostRenderer.utf8ToAscii, PostRenderer.formatDate, AppNotification success/error/info.

## [7.5.2] - 2026-04-20

### Fixed
- **REST API deactivation bypass.** API endpoints (`/api/v1/in_progress`, `/start_from_app`, `/finish_from_app`, etc.) now check active status via `resolveActiveUser()`. Deactivated users get 403 Forbidden instead of full API access.
- **Email case normalization.** Admin invite dialog normalizes email to lowercase before uniqueness check, matching `UserServiceImpl.inviteUser()` behavior.

## [7.5.1] - 2026-04-20

### Added
- **Member deactivation UI.** Admin Members tab shows all users (active first, then inactive) with Deactivate/Reactivate action buttons. Admins cannot deactivate themselves. Deactivated users are soft-deleted (post history preserved) and excluded from email notifications, post selectors, and Go-To-Guy pair assignments.
- **Deactivated user hard block.** Deactivated users see an error page after Keycloak auth: "Your account has been deactivated. Contact your group admin." No app access until reactivated.
- **`AppNotification` utility class.** Centralized notification display with consistent MIDDLE position, 3s success / 5s error duration, and LUMO theme variants. Replaces all `Notification.show()` calls across PostView, AdminView, ProfileView, and StatsView.
- `deactivateUser()`, `reactivateUser()`, and `getAllUsers()` methods on UserService.
- 5 new tests for deactivation, reactivation, active-check blocking, and getAllUsers delegation.

### Changed
- Moved Email Invitation Flow and Per-User Billing Integration from P1 to P2 in TODOS.md (require external service setup).
- Added P3 "Deactivated User Read-Only Mode" task to TODOS.md for future refinement.

### Fixed
- Notifications hidden behind nav drawer (AdminView email validation) now display at MIDDLE position.
- "New post started!" notification now appears (was silently lost with `Notification.show()`).

## [7.5.0] - 2026-04-20

### Added
- **Deep-linkable posts.** `/posts/{postId}` URL parameter loads a specific post directly. Works for own in-progress posts, own completed posts, and other group members' completed posts. Other users' in-progress posts are blocked with a notification.
- **Email notification deep links.** Activity notification emails now include a direct `/posts/{postId}` link instead of a generic homepage link. Clicking the link authenticates via Keycloak and lands on the specific post.
- **Post-login redirect.** Vaadin's `VaadinSavedRequestAwareAuthenticationSuccessHandler` preserves the original URL through the Keycloak OAuth2 flow, so unauthenticated deep link clicks land on the correct post after login.
- `getPostById()` method on `VaadinPostPresenter` for post lookup by ID.
- 8 new tests covering deep link routing: completed posts, own in-progress posts, other users' completed posts, blocked in-progress posts, post-not-found, null parameter default behavior.

### Changed
- **Local dev setup.** Added `nginx/sites/tnra-dev.conf` to route `tnra.dev.dogeared.dev` through nginx to the IDE on `host.docker.internal:8080`. Updated Keycloak realm with HTTPS redirect URIs for the new dev hostname.
- `ActivityNotificationRenderer` strips trailing slash from `baseUrl` and handles null post ID gracefully.
- Added `Normalize Notifications` task to TODOS.md (P1) for standardizing notification position, duration, and display pattern across all views.

## [7.4.3] - 2026-04-19

### Added
- 5 regression tests for `flushPendingValues` stat sync: card differs from DB, card matches DB (no-op), card cleared to null, multiple mismatches, null post safety. Added `setValueSilently()` to StatCard for simulating Vaadin event batching in tests.

## [7.4.2] - 2026-04-18

### Fixed
- **Finish Post fails with "stats - exercise" even after entering all stat values.** The stat value change events could arrive at the server out of order or be lost entirely due to Vaadin event batching, causing the database to be out of sync with the UI when finish validation runs. Now `flushPendingValues()` explicitly syncs all StatCard UI values to the database before finish validation, regardless of whether value change events fired. Also backported to v4.14.7.

## [7.4.1] - 2026-04-18

### Added
- 64 new tests raising JaCoCo instruction coverage from 70% to 83%.
  - ProfileView (28): phone formatting/validation/normalization, saveProfile, personal stat CRUD (add, move, archive, restore).
  - AdminView (16): global stat management, member invitation with email validation, validation error display.
  - MainLayout (10): dark mode cookie read/write, theme toggle, initial dark mode resolution, admin role detection.

## [7.4.0] - 2026-04-18

### Added
- **Admin user provisioning**: CLI now requires `--admin-email`, `--admin-first-name`, `--admin-last-name`. Generates a Keycloak user with temporary UUID password (must change on first login) and `seed-admin.sql` to insert the user into the app database.
- **MySQL init script** (`mysql/init-local-user.sql`): auto-creates the `tnra` database user matching `application.yml` defaults on first container start.
- **Nginx catch-all server block** returning 444 for unrecognized hostnames, preventing hostname enumeration.
- **Keycloak reverse proxy** at `auth.dev.dogeared.dev` via nginx, with `KC_HOSTNAME` set for consistent token issuer across browser and container access.
- **Placeholder image provisioning**: CLI creates `uploads/<group>/placeholder.png` during provisioning, copied from project root.
- **V6 migration**: converts `post.state` from TINYINT to VARCHAR for Hibernate 6 (enum default changed from ORDINAL to STRING).
- **Seinfeld demo users** in base Keycloak realm: Jerry (admin), Kramer, Elaine, George (member).
- 3 new profile image upload tests (regression + edge cases).

### Changed
- **`.env.template`** converted from shell `export` format to plain `KEY=VALUE` for Docker Compose compatibility. Organized by category with `MYSQL_ROOT_PASSWORD` as independent variable.
- **CLI `docker-compose.yml` template** uses `env_file` for credentials instead of hardcoding secrets. Docker-internal URLs for MySQL and Keycloak backend endpoints set as environment overrides.
- **CLI `.env` template** uses host-accessible URLs (`localhost:3307`, `https://auth.DOMAIN`) for IDE development.
- **CLI instructions template** covers both IDE dev (Step 4a) and Docker container (Step 4b) paths.
- **Nginx per-group template**: removed duplicate `map $http_upgrade` block (defined once in default config).
- **WebSocket upgrade headers** added to nginx localhost server block.
- **Keycloak base realm**: disabled self-registration, replaced `postLogoutRedirectUris` with Keycloak 26 `attributes` format.
- **V1 migration** made idempotent for pre-existing databases (conditionally adds `dark_mode` column).
- README rewritten for unified provisioning flow with group removal instructions.
- Local dev domain changed to `dev.dogeared.dev` (real TLD avoids browser cookie restrictions).

### Fixed
- **Profile image upload not persisting**: `processProfileImageUpload` stored the file on disk but never called `saveUser`, leaving `profile_image` NULL in database.
- **`MYSQL_ROOT_PASSWORD`** decoupled from `SPRING_DATASOURCE_PASSWORD` (were incorrectly shared).
- Dead env vars removed from server service (`MYSQL_DATABASE`, `MYSQL_HOST`, `MYSQL_PORT`).
- Per-group Docker compose build context set to `../..` (project root has the Dockerfile).

### Removed
- Old test accounts (`admin@tnra.local`, `member@tnra.local`) replaced by Seinfeld demo users.

## [7.3.0] - 2026-04-02

### Added
- **Provisioning CLI** for multi-group support. Run `java -jar tnra-cli.jar provision <group-name> --domain tnra.app` to generate all config files for a new group: Docker Compose, Keycloak realm, Nginx server block, MySQL init script, environment variables, and operator instructions.
- **Shared Docker network** (`tnra-shared`) so per-group containers can reach MySQL and Keycloak by hostname.
- **Per-group Nginx routing** via `nginx/sites/` directory with `include` directive for subdomain-based routing.
- **Group registry** (`groups.json.example`) tracks provisioned groups with auto-assigned ports.
- **CLI CI job** in GitHub Actions (`cli-test`) runs CLI tests on push and PR.
- Multi-group local development and production deployment docs in README.md and PRODUCTION.md.

### Changed
- `.env.template`: removed stale Okta vars, added Keycloak and Vaadin production mode vars.
- `.gitignore`: added `provision/` (CLI output) and `groups.json` (deployment state).

## [7.2.1] - 2026-04-02

### Changed
- Removed stale `VAADIN_SETUP.md` (content already covered by README.md, contained outdated redirect URI and debug logging info).
- Fixed auth config fallback reference from Okta to Keycloak in README.md.
- Added Branch 5 encryption plan to TODOS.md (app-level AES-256-GCM per-tenant keys, landing page with request access form).

## [7.2.0] - 2026-04-02

### Security
- **Keycloak logout now revokes the Keycloak session.** Previously, logout only cleared the Spring session. Clicking "Login" after logout would silently re-authenticate. Now uses `OidcClientInitiatedLogoutSuccessHandler` with `issuer-uri` for OIDC RP-Initiated Logout.
- **Narrowed Keycloak redirect URI** from wildcard `/*` to exact match `/login/oauth2/code/keycloak`, closing an open-redirect vulnerability.
- **Moved DEBUG logging to local profile.** Production no longer logs Spring Security tokens, OAuth2 headers, or PII. DEBUG levels are in `application-local.yml` only.
- **Session timeout reduced from 30 days to 24 hours.**

### Fixed
- `emailTaskExecutor` now uses `CallerRunsPolicy` instead of `AbortPolicy`. Emails are no longer silently dropped when the queue fills. Graceful shutdown waits up to 30 seconds for in-flight emails.
- Added `issuer-uri` to Keycloak provider config and `postLogoutRedirectUris` to realm export, enabling OIDC logout discovery.

### Added
- `application-local.yml.sample` for local-only DEBUG logging configuration.
- Test coverage section in `CLAUDE.md` (minimum: 60%, target: 80%).
- 23 new tests: `EmailServiceImplTest` (4), `PostServiceImplTest` (16), `NotFoundViewTest` (3).

## [7.1.1] - 2026-03-31

### Changed
- Consolidated 7 scattered markdown files (Dockerize, DatabaseSetup, HostingSetup, FILE_UPLOAD_SETUP, Vaadin_Cursor_Notes, SUBDOMAIN_SETUP, post-entity-diagram) into two comprehensive docs.
- **README.md** rewritten with architecture overview, entity model, three local development options (H2, MySQL, full Docker Compose), Keycloak setup, test instructions, Flyway migration reference, and project structure.
- **PRODUCTION.md** created with VPS provisioning, SSL certificate management, build/deploy workflow, systemd service setup, firewall rules, MySQL hardening, Keycloak hardening, and database migration from V1.

### Removed
- `Dockerize.md`, `DatabaseSetup.md`, `HostingSetup.md`, `FILE_UPLOAD_SETUP.md`, `Vaadin_Cursor_Notes.md`, `nginx/SUBDOMAIN_SETUP.md`, `post-entity-diagram.md`

## [7.1.0] - 2026-03-29

### Security
- **Admin view access control enforced**: non-admin users could access the Admin Dashboard by navigating directly to `/admin`. Replaced manual Spring Security filter chain with `VaadinSecurityConfigurer`, which enforces `@RolesAllowed` annotations on Vaadin views at the framework level.
- Custom "Page not found" view replaces default Vaadin error page that leaked all available route paths.
- Simplified role mapping to Keycloak-only `realm_access.roles` extraction, removing legacy Okta/Auth0/generic IdP fallback code.

### Fixed
- Daily Calls route changed from `/gtg` to `/daily-calls` to match the nav label. Bookmarks and direct URLs now work.

### Changed
- Added `@PermitAll` to StatsView, PostView, DailyCallsView, ProfileView (required by `VaadinSecurityConfigurer` — unannotated views are denied by default).
- Renamed `GTGView` to `DailyCallsView` to align class name with route and nav label.

## [7.0.0] - 2026-03-29

### Added
- **Personal stats**: members can create, archive, restore, and reorder their own stats from the Profile page. Personal stats appear after global stats in the post form and are visible to all group members on completed posts.
- SINGLE_TABLE JPA inheritance on StatDefinition with discriminator column (`scope = GLOBAL | PERSONAL`). PersonalStatDefinition extends StatDefinition with a user FK.
- Bidirectional name collision prevention: personal stat names can't match global stat names (active or archived). Admin can't create a global stat matching any active personal stat name. Restoring an archived personal stat is blocked if a global stat now has that name.
- Grace period for mid-post stat changes: stats created after a post was started are not required for that post.
- Completed posts derive their stat list from saved PostStatValue entries (shows the post author's stats, not the viewer's).
- Clear enabled/disabled styling for the Finish Post button (green accent when enabled, grayed out when disabled)
- Floating success notification after completing a post

### Fixed
- NPE in `Post.getStatValue()` when a PostStatValue exists but its value is null
- V5 migration: correct table name (`users` not `user`) and index name (`uk_stat_definition_name`)

### Changed
- All StatDefinitionRepository queries are now global-scoped (old unscoped queries deleted). Every call site in AdminView, PostServiceImpl, and VaadinPostPresenterImpl updated.
- `StatsView.createEmbedded()` now takes a User parameter to load personal stats
- `PostServiceImpl.ensureStats()` loads global + personal stats for the post's user
- Email unique constraint added via V5 migration (bundled from TODOS P0)
- Stat cards without emojis now render the same height as those with emojis

## [6.0.4] - 2026-03-29

### Changed
- Renamed "Go To Guy" menu item and view header to "Daily Calls"

## [6.0.3] - 2026-03-29

### Fixed
- **application.yml untracked from git**: credentials no longer committed to the public repo. Added `application.yml.sample` with placeholders for new developer setup.
- Updated README with copy-and-configure instructions for local development

## [6.0.2] - 2026-03-28

### Fixed
- **IDOR vulnerability**: POST /api/v1/in_progress now enforces ownership — authenticated users can only update their own in-progress post
- **Hardcoded DB password**: replaced with environment variable reference (no default, fails loud if unset)
- **Dockerfile runs as root**: added non-root `appuser` (UID 1001)
- **Keycloak SSL**: changed `sslRequired` from `none` to `external`
- Added `@JsonProperty(READ_ONLY)` on Post.id and Post.user to prevent mass-assignment via JSON

## [6.0.1] - 2026-03-27

### Added
- Custom Keycloak login theme matching TNRA's organic/grounded design system (warm linen background, forest green buttons, system sans-serif fonts)
- Docker volume for Keycloak data persistence between container restarts

## [6.0.0] - 2026-03-27

### Added
- Keycloak OIDC authentication replacing Okta (realm_access.roles extraction with flat claim fallback)
- Activity-only email notifications via ActivityNotificationRenderer (no post content in emails)
- Per-user notification preference (notifyNewPosts) with V4 Flyway migration
- Email notification toggle in ProfileView
- Admin-controlled member invitations (Members tab in AdminView with invite dialog)
- Local Keycloak development environment in Docker Compose with pre-configured tnra realm
- Spring @Async email sending with bounded ThreadPoolTaskExecutor (5 threads)
- P1 TODOs for email invitation flow, per-user billing, member deactivation

### Changed
- Auth provider: Okta → Keycloak (spring-boot-starter-oauth2-client, env var config)
- Email content policy: full post content → activity-only summaries (security policy)
- User creation: auto-creation on OIDC login disabled → admin must invite by email first
- Email kill switch moved into EmailServiceImpl (covers both Vaadin and API callers)
- First OIDC login populates name from claims if invited user has email-only record

### Removed
- okta-spring-boot-starter dependency
- Okta configuration from application.yml, docker-compose.yml, README, VAADIN_SETUP.md
- Auto-creation of user records on first OIDC login (replaced by admin invite)
- Raw ExecutorService in EmailServiceImpl (replaced by Spring @Async)

## [5.2.0] - 2026-03-27

### Added
- DESIGN.md — living design system documentation with color palette, typography, spacing, layout, and motion specs
- Inline `<style>` prepended to HTML head via AppShellConfigurator to prevent Vaadin/Lumo font flash

### Changed
- Complete color palette overhaul: forest green (#2D6A4F) primary, warm linen (#FDFBF7) backgrounds, dark goldenrod (#B8860B) accent
- Typography: all system sans-serif with weight-based heading hierarchy (700/600), replacing Inter + generic blue
- Lumo variables overridden in both light and dark mode (primary, error, success, text, font family, font sizes)
- Dark mode: warm brown-blacks (#1A1610) instead of cool blue-blacks
- Nav sizing: 64px navbar, 1rem drawer tabs, 1.5rem logo, 44px touch targets
- All inline `getStyle().set()` calls moved to external CSS classes
- All `LumoUtility` font/text classes replaced with custom CSS classes
- Monospace font: system monospace stack (ui-monospace, SF Mono, Menlo, Consolas)

## [5.1.0] - 2026-03-27

### Added
- Admin "Stats Config" tab for managing stat definitions (add, archive, restore, reorder)
- Up/down arrow buttons for reordering active stats
- Soft-delete (archive) for stats with "at least one active stat" validation
- Restore button for archived stats (appends to end of active list)
- Add Stat dialog with name, label, emoji fields and duplicate name validation
- Archived stats shown greyed with badge in the admin list

### Changed
- Stat card control buttons bumped from 34px to 44px for WCAG touch target compliance

## [5.0.0] - 2026-03-27

### Added
- Dynamic `StatDefinition` model replacing hardcoded 7-field Stats embeddable
- `PostStatValue` join table for per-post stat values
- V3 Flyway migration: creates new tables, seeds default stats, migrates existing data, drops old columns
- `StatDefinitionRepository` and `PostStatValueRepository` for stat management
- `Post.getStatValue(name)` and `Post.setStatValue(def, value)` convenience methods
- Production migration plan document (`MIGRATION-V3-STATS.md`)

### Changed
- `Post` entity: `@Embedded Stats` replaced with `@OneToMany PostStatValue` relationship
- `PostService`: `replaceStats()`/`updateCompleteStats()` replaced with `updateStatValue()`
- `VaadinPostPresenter`: new `updateStatValue()` and `getActiveStatDefinitions()` methods
- `StatsView` renders dynamically from active `StatDefinition` entries
- `EMailPostRenderer` renders stats dynamically from `PostStatValue` list
- `Post.toString()` formats stats dynamically
- `.gitignore`: Flyway migration SQL files now explicitly allowed

### Removed
- `Stats` embeddable class (replaced by dynamic model)
- `CommandParser` and `Command` model classes (dead Slack code from Branch 1)
- `replaceStats()` and `updateCompleteStats()` from PostService interface

## [4.15.0] - 2026-03-26

### Added
- Flyway versioned database migrations replacing Hibernate `ddl-auto: update`
- V1 baseline migration capturing existing schema
- V2 migration making Slack columns nullable and dropping PQ token columns
- `getUserDisplayName()` helper decoupling error messages from Slack usernames
- Branch-by-branch deployment guide in README

### Removed
- Slack integration (slash commands, broadcast channel, API service, renderers)
- PQ metrics feature (controller, service, renderer, scheduled refresh)
- SMS/text notifications (carrier email-to-text via Mailgun)
- Dead `findBySlackUserId`/`findBySlackUsername` repository methods
- PQ token fields from User entity (`pqAccessToken`, `pqRefreshToken`)

### Changed
- `ddl-auto` switched from `update` to `validate` (schema changes now via Flyway only)
- Security config: removed public access to Slack API and PQ endpoints
- docker-compose: added Flyway env vars, removed Slack/PQ/SMS env vars
- README rewritten with local dev, Flyway, and production deployment docs

### Fixed
- Pre-existing AdminViewTest Vaadin UI context failure for notification display
- `getUserDisplayName()` handles blank strings, not just null

## [4.14.6] - 2026-03-17

### Fixed
- AppLayout nav and post header spacing on mobile

## [4.14.5] - 2026-03-14

### Changed
- Raised test coverage above 80%
- Updated badge references in README

## [4.14.4] - 2026-03-13

### Changed
- Simplified badge commit workflow in CI

## [4.14.3] - 2026-03-13

### Changed
- Refactored AdminView to use VaadinAdminPresenter pattern (MVP)

## [4.14.2] - 2026-03-12

### Fixed
- Replaced deprecated Vaadin MemoryBuffer upload usage

## [4.14.1] - 2026-03-12

### Changed
- Themed logout confirmation dialog for light and dark mode
- Submit logout directly from themed dialog

## [4.14.0] - 2026-03-12

### Added
- Build info tab in AdminView (Git tag, commit, branch, versions, build time)

## [4.13.4] - 2026-03-12

### Changed
- Updated Vaadin to latest version

## [4.13.3] - 2026-03-12

### Changed
- Updated Java to 21 in Dockerfile

## [4.13.2] - 2026-03-12

### Fixed
- Generated badge filenames and JaCoCo badge path

## [4.13.1] - 2026-03-11

### Added
- Tests passing and coverage badges in README
- Focused tests to raise coverage

## [4.13.0] - 2026-03-11

### Added
- Configurable login route via `AuthNavigationService`
- Login CTA on main view
- PR test workflow in GitHub Actions
- Auth navigation documentation in README

## [4.12.0] - 2026-03-11

### Added
- Modern UI theme with light/dark mode toggle
- Typography-driven hierarchy for post page
- TNRA logo in header next to hamburger menu

### Changed
- Comprehensive Lumo variable overrides for dark mode contrast
- Post page redesigned with input improvements

### Fixed
- Dark mode toggle to set theme attribute on html element
- PQ auth flow hardened and main view resiliency improved

## [4.11.0] - 2026-02-25

### Changed
- Updated Spring Boot version
- Updated Vaadin version for security fix

### Added
- Database setup documentation
- Utilities to recreate post records from emails

## [4.10.1] - 2025-12-06

### Fixed
- Docker Compose binds only to localhost, uses password env var

## [4.10.0] - 2025-11-09

### Added
- User selector dropdown to view other members' previous posts

## [4.9.0] - 2025-11-08

### Added
- Go To Guy (GTG) call chain management in admin
- Tabbed admin interface
- Groups claim extraction for admin role authorization

## [4.8.1] - 2025-08-11

### Added
- Vaadin component test coverage raised to 80%

## [4.8.0] - 2025-08-10

### Changed
- Refactored VaadinPostService to VaadinPostPresenter (MVP pattern)
- Moved presenter into `vaadin/presenter` package

## [4.7.1] - 2025-08-07

### Fixed
- Made uploads folder a Docker volume for persistence

## [4.7.0] - 2025-08-05

### Added
- Sticky header for PostView
- Email and Slack services made optional (configurable, true by default)

### Changed
- Finish button moved to bottom of post form
- Set timezone to UTC by default
- Optimized imports across the project

### Fixed
- Clear currentPost before clearing form to prevent cascade write to DB

## [4.6.0] - 2025-07-27

### Changed
- Refactored PostView to use Vaadin Binder for all fields (replacing manual syncing)

## [4.5] - 2025-07-23

### Fixed
- Times displayed in local timezone using browser utilities

## [4.4] - 2025-07-23

### Fixed
- Null stats values now properly written to database
- Centralized date utilities for local timezone display

## [4.3] - 2025-07-22

### Changed
- Refactored PostView for better mode rendering (in-progress vs completed)
- Moved start time out of StatsView onto PostView
- GTGView shows dates in local timezone

### Fixed
- Stats bug with updates on embedded view and persisting nulls

## [4.2] - 2025-07-18

### Added
- Profile view with phone number validation and image upload
- GTG (call chain) view
- Pagination controls for completed posts
- Post form with auto-sync to database
- StatsView as embeddable component in PostView

### Changed
- All styles externalized from Java code into CSS files
- ErrorView refactored to use external CSS

### Fixed
- Start new post button clears dropdown and form data
- Read-only toggle handled automatically in StatsView

## [4.1] - 2025-07-18

### Changed
- Updated Docker, Nginx, and app configuration for production deployment

## [4.0] - 2025-07-15

### Added
- Vaadin Flow frontend (replacing Vue.js)
- OAuth2/OIDC authentication with Okta
- Post creation and viewing
- Stats tracking with increment/decrement controls
- Hamburger menu navigation
- StatsView with stat cards

### Removed
- Vue.js frontend components and frontend build from pom.xml

## [3.1] - 2024-11-24

### Changed
- Updated Maven wrapper

## [3.0] - 2024-11-24

### Changed
- Upgraded to Spring Boot 3.x
- Updated User entity for JPA compatibility
- Added Hibernate validator
- Migrated from Heroku to Vultr VPS deployment
