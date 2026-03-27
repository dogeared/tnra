# Changelog

All notable changes to TNRA are documented in this file.

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
