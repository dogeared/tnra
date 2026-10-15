# TODOS

## P1 — Next big features

### Externalize Keycloak — use the centralized Keycloak server  *(in progress)*
**Status:** the central project exists — **dogeared/keycloak** (compose, phase-2 Postgres compose,
migrate/backup scripts, systemd unit, runbook). TNRA 10.7.0 removed the `keycloak` service and volume
from `docker-compose.production.yml`. Remaining: run the Phase 1 cutover on .104 (that repo's README),
then Phase 2 (production mode on Postgres), then strike this entry.

TNRA ships its own Keycloak container (dev `docker-compose.yml`, `docker-compose.production.yml`) and the
provisioning CLI creates one realm per group in it from `realm.json.tmpl`. A centralized Keycloak server
already exists that serves several apps, each with its own realm template. Move TNRA's identity there and
take Keycloak out of this project entirely.
- **Why:** One identity server to patch, back up and monitor instead of one per deployment; the per-group
  realm template becomes just another template on the central server; the `auth.<domain>` hostname, the
  `keycloak` compose service, its volume and its tunnel route all go away.
- **Effort:** M (human: ~2-3 days incl. migrating the-og's realm / CC: ~1 h)
- **Context:** The group app only needs an issuer: `KEYCLOAK_ISSUER_URI` / the
  `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_*` env in `docker-compose.yml.tmpl` already point at
  `https://auth.<domain>/realms/<realm>` for the browser-facing URI, but the token/userinfo/jwk URIs use the
  Docker-internal `http://keycloak:8080` — those must become the central server's public URL (or a private
  one it exposes). `realm.json.tmpl` (roles, client, admin user with `UPDATE_PASSWORD`, SMTP block, `tnra`
  login theme) is the realm template to register centrally; the `tnra` login theme must be installed there.
  Migrate the-og by exporting its realm (users + credentials) and importing it centrally, then flipping the
  env and the CSP `form-action` origin follows automatically (derived from the authorization URI). Dev site:
  point at a dev realm on the central server (or keep a throwaway Keycloak in the dev compose only). Remove
  `KEYCLOAK_*` from `.env.template`, the `keycloak` service + `keycloak-data` volume, `KC_HOSTNAME` notes and
  the `auth` tunnel route from the runbooks. Do this BEFORE the provisioning work below so provisioning
  targets the central server's admin API once.

### Provision a new group as automatically and easily as possible
**Plan:** see `PROVISIONING_PLAN.md` (steps, barriers with fixes, delivery order). Status: planned 2026-10-15.

Today `tnra-cli provision` *generates files* (`provision/<group>/…`) and prints an `instructions.md` whose
steps are done by hand: run `init-db.sql`, import the realm into Keycloak, add a tunnel route, `docker
compose up` with the overlay, run `seed-admin.sql`, hand over the temporary password. Make it one command
that *does* those steps, idempotently, and leave it callable as a service so a self-serve flow can call it.
- **Why:** Every new group costs founder time and is error-prone (ordering, the "run from `~/tnra`" trap,
  copying tokens by hand). Automated provisioning is the prerequisite for self-serve growth.
- **Effort:** L (human: ~1-2 weeks / CC: ~hours)
- **Depends on:** Externalize Keycloak (above) — so realm creation is a single admin-REST call against the
  central server rather than a file drop + container restart.
- **Context:** `ProvisionCommand` already renders everything and registers the group with billing (`POST
  /api/admin/groups`); extend it to *execute*: create DB + user over JDBC (root creds from `.env`), create
  the realm via Keycloak's admin REST API (`POST /admin/realms` with the rendered JSON), create the tunnel
  public hostname via Cloudflare's API (tunnel config endpoint; container-name target once `cloudflared`
  runs in Docker — see Tech debt), `docker compose … up -d` the overlay, run the admin seed, and print one
  summary. Each step checks before it creates so a re-run after a partial failure is safe. Put the
  per-group billing token in `.env` as `TNRA_BILLING_API_TOKEN_<GROUP>` (see `.env.template`) rather than in
  the overlay. Then wrap the same code as a service (`tnra-billing-app` or a small `tnra-provisioner`) for
  the self-serve end state below. `instructions.md.tmpl` shrinks to "what was done + how to verify".

### Self-serve onboarding — leader signup → pay → auto-provision  *(end state of the item above)*
Turn group creation from a CLI operation into a self-serve flow: a leader signs up on the landing site,
pays, and a group is provisioned automatically (DB + Keycloak realm + subdomain), no CLI step.
- **Why:** This is the confirmed product goal (self-serve growth). The billing app is its foundation;
  this is the next platform bet that makes groups onboard without the founder touching the CLI.
- **Effort:** L (human: ~1-2 weeks / CC: ~hours) — mostly delivered by the automation item; what remains is
  the landing-side flow (signup → Paddle checkout → webhook → call the provisioner) and the guardrails.
- **Depends on:** `tnra-billing-app` shipped; provisioning callable as a service (above).
- **Context:** See CEO plan `~/.gstack/projects/dogeared-tnra/ceo-plans/2026-06-14-billing-monetization.md`.
  Trigger on a successful first payment, safely and idempotently.

## P1.5 — Branch 5 (Next Up)

### ~~Deploy Encryption to Active Group~~ ✓ Completed (ops)
Roll out v8.0.3 to the current active group for real-world testing before new features land.
- **Why:** Validates encryption migrations, JPA converters, and master key config in production before the user base grows.
- **Effort:** XS (human: ~1 hour / CC: n/a — ops task)
- **Context:** Run Flyway V7–V10 migrations against the live DB. Confirm encrypted columns read/write correctly via the UI. Master key must be set in the group's `.env`. This is a gate before Slack Part 1 ships.

### ~~Slack Integration — Part 1: Activity-Only with Post Link~~ ✓ Completed v8.1.1
Admin-configurable incoming webhook for a Slack channel. When a member finishes a post, post a message to the configured channel containing: username, start time, finish time, and a deep link to the post.
- **Why:** Gives groups immediate visibility into posting activity without exposing encrypted post content outside the DB. Deep link enforces authentication before content is visible.
- **Effort:** S (human: ~2 days / CC: ~20 min)
- **Depends on:** Encryption deployed to active group. Deep links shipped (v7.5.0). `group_settings` table for storing webhook URL + enabled flag.
- **Context:** Use Slack incoming webhooks (no OAuth, no slash commands). One webhook URL per group, stored encrypted in `group_settings`. Admin UI in the Admin panel to configure the webhook URL and toggle on/off. Message format: `[username] finished a post | Started: [time] | Finished: [time] | View: [deep link URL]`. No post content, no stats — activity signal only.

### ~~Deep Link URL Token — Obfuscate Post ID in Shared Links~~ ✓ Completed v8.1.0
Replace the raw database ID in post deep links (`/post/42`) with an AES-GCM-encrypted, base64url-encoded token so sequential IDs are never exposed externally.
- **Why:** Sequential integer IDs in URLs allow enumeration attacks — an authenticated user can increment the ID to probe posts that aren't theirs. Slack notifications make this worse by broadcasting the link to an entire channel.
- **Effort:** S (human: ~1 day / CC: ~15 min)
- **Depends on:** Slack Part 1 shipped. Encryption infrastructure already in place (V7/V8 migrations, `EncryptedStringConverter`).
- **Context:** Reuse the existing AES-256-GCM service to encrypt the `Long` post ID (serialize as a string, encrypt, base64url-encode for URL safety). Expose a `PostTokenService` (or method on the existing crypto service) with `encode(Long id)` and `decode(String token)`. Update the PostView router to decode the token before fetching. Update `SlackNotificationServiceImpl.buildMessage()` to encode the ID when constructing the deep link. The existing `/post/{id}` route stays but checks ownership; the token route is the new public-facing path. Decryption failure or post-not-found both return 404 — no information leakage.

### ~~Landing Page with Request Access Form~~ ✓ Completed v9.1.0
Static or Vaadin public route for prospective groups. Form: group name, contact name, email, estimated size, description. Submissions stored in `request_access` table + email notification to founder.
- **Why:** Need somewhere to point prospective groups. Supports go-to-market.
- **Effort:** S (human: ~2 days / CC: ~15 min)
- **Depends on:** Slack Part 1 shipped.
- **Context:** Requires Spring Security rule for anonymous access without exposing other routes. Rate limiting on form (max 5/hour per IP).

### ~~Temporary Password Change Not Enforced on First Login~~ ✓ Completed (realm.json.tmpl sets `requiredActions: ["UPDATE_PASSWORD"]` on the admin user)
When the provisioning CLI creates the initial admin user in `realm.json.tmpl` (imported into Keycloak during group setup), the password is marked temporary but Keycloak does not prompt the user to change it on first login.
- **Why:** Temporary passwords are a security control — if Keycloak never challenges the user to change it, the provisioned credential stays active indefinitely.
- **Effort:** XS (human: ~30 min / CC: ~10 min)
- **Context:** The admin user is defined in `cli/src/main/resources/templates/realm.json.tmpl` with `"temporary": true`. Keycloak enforces a password change on first login via `requiredActions: ["UPDATE_PASSWORD"]` on the user record. Check whether this field is present and correctly set in the realm template. May also need to confirm the Keycloak realm has the `UPDATE_PASSWORD` required action enabled globally.

### ~~Clean Up Provisioning CLI Templates~~ ✓ Completed v10.6.2 (deeper restructuring lands with the Keycloak + provisioning features)
Review and tighten all templates in `cli/src/main/resources/templates/` for correctness, consistency, and completeness before onboarding additional groups.
- **Why:** Templates have been iterated rapidly during production hardening and may contain stale comments, inconsistent formatting, missing settings, or values that should be configurable but are currently hardcoded.
- **Effort:** S (human: ~1 day / CC: ~20 min)
- **Context:** Templates to review: `realm.json.tmpl`, `docker-compose.yml.tmpl`, `env.tmpl`, `instructions.md.tmpl`, `nginx.conf.tmpl`, `init-db.sql.tmpl`, `seed-admin.sql.tmpl`. Check for: hardcoded values that should be CLI options, missing Keycloak realm settings (e.g. session timeouts, brute-force protection), INSTRUCTIONS.md accuracy against actual deployment steps, and any template vars that are set but unused (or used but not set).

### ~~Default the `founder-email` Notify Property~~ ✓ Completed v10.6.1 (the nginx sites-split sub-note remains a dev-stack nicety, not done)
Add a default to `@Value("${tnra.notify.founder-email}")` in landing's `MailgunNotificationServiceImpl` (e.g. `${tnra.notify.founder-email:tnra@dogeared.dev}`).
- **Why:** It's the only notify `@Value` with no default, so a missing/mis-named env var (Spring relaxed binding expects `TNRA_NOTIFY_FOUNDER_EMAIL`) takes the whole landing context down at startup instead of degrading gracefully. This bit us in local dev when docker-compose set `TNRA_FOUNDER_EMAIL` — fixed the env var name, but the brittle binding remains.
- **Effort:** XS (CC: ~5 min)
- **Context:** Do this on the module/parent-pom branch alongside config consolidation. Consider also splitting the nginx `include /etc/nginx/sites/*.conf` so provisioned-tenant confs (`tnra1.conf`, `tnra2.conf`) live in a separate dir from the dev stack — stray tenant upstreams currently crash the dev `proxy`. Those two confs are parked in `nginx/sites-disabled/`.

### ~~Slack Integration — Part 2: Stats and Full-Post Tiers~~ ✓ Completed v8.3.0 (group master toggle + body/stats tiers, per-member opt-in)
Extend Part 1 with two additional admin-selectable content tiers: stats-only (username + stat values, no narrative text) and full-post (all post sections, decrypted at send time). Admin selects tier per group.
- **Why:** Groups that are comfortable with the trade-off can opt into richer Slack notifications.
- **Effort:** S-M (human: ~2 days / CC: ~20 min)
- **Depends on:** Slack Part 1 shipped, landing page shipped, at least one group providing feedback on Part 1.
- **Context:** Tier selection stored in `group_settings`. Full-post tier decrypts content in-memory before sending to Slack — clear security warning in the admin UI that post content will leave the encrypted DB. Stats-only tier sends stat names + values only (no narrative). Slack message layout adapts per tier.

### ~~Slack integration guide in the admin panel~~ ✓ Completed v10.6.6
In-app guidance (an Admin panel tab/section) that walks an admin through setting up the Slack incoming
webhook: where to create it in Slack, what URL to paste, and how the per-group publish toggles behave.
- **Why:** The webhook is created in Slack's own UI, not ours — without guidance admins get stuck or
  paste the wrong value. A guide cuts setup friction and support questions.
- **Effort:** S (human: ~half day / CC: ~15 min)
- **Depends on:** Slack Integration Part 1 shipped (the webhook config UI this documents).
- **Context:** Lives next to the Slack/Integrations config in `AdminView`. Step-by-step for Slack's
  "Incoming Webhooks" flow (create app → enable incoming webhooks → add to channel → copy URL), a note
  that the URL is stored encrypted, and what each publish toggle (activity / stats / full-post) sends.
  Could be a collapsible help section or a dialog. Read DESIGN.md before styling.

### ~~Billing — "comp_until expiring soon" reminder~~ ✓ Completed v10.6.4 (covers group-trial end too, for members with a billing row)
Scheduled job in `tnra-billing-app` that emails a member ~7 days before their local `comp_until`
waiver (e.g. "first 3 months free" promo) expires, so they're nudged to add a card before access flips.
- **Why:** With local time-boxed waivers (not Lemon Squeezy trials), there's no card on file, so at
  expiry the member must actively come pay. A reminder reduces conversion drop-off at the cliff.
- **Effort:** XS (human: ~2h / CC: ~10 min)
- **Depends on:** Billing feature shipped (`tnra-billing-app`). Deferred out of v1 — v1 enforcement is
  lazy-at-gate (member hits PENDING_PAYMENT on next login after expiry), which needs no scheduler.
- **Context:** See `BILLING_PLAN.md`. Add a `@Scheduled` sweep over `billing_account.comp_until`
  within 7 days and not yet reminded; send via the existing mail path. Mark reminded to avoid repeats.

### ~~Billing — bulk "cover all uncovered members" gift~~ ✓ Completed v10.6.5 (chained checkouts — Paddle needs one per subscription)
One-click admin action to gift-cover every not-yet-covered member at once (vs gifting one at a time).
- **Why:** Convenience for a founder/ministry leader sponsoring a whole group; reduces N clicks to 1.
- **Effort:** S (human: ~half day / CC: ~10 min)
- **Depends on:** Billing gift subscriptions shipped (v1 supports single-beneficiary gifting already).
- **Context:** See `BILLING_PLAN.md` gift section. Iterate uncovered members → create a gift checkout per
  member under the payer's card. Deferred from v1 because one-at-a-time gifting already delivers the
  capability; this is pure convenience.

### ~~Billing — Paddle-side "already subscribed" check at checkout~~ ✓ Completed v10.6.3
Before creating a self-pay checkout, ask Paddle whether the member's email already has a live subscription
and short-circuit with the same 409 the local duplicate-charge guard in `CheckoutService` uses.
- **Why:** The local guard reads our own DB, which is authoritative in steady state. It can't catch a case
  where our DB diverged from Paddle (e.g. webhooks were down so the account still shows PENDING while
  Paddle already has a paid subscription) — exactly the situation that allowed a double charge during local
  testing. A provider-side lookup closes that gap. (Originally written for Lemon Squeezy; billing moved to
  Paddle in 10.1.0.)
- **Effort:** S (human: ~half day / CC: ~20 min)
- **Depends on:** Billing shipped. Adds one or two Paddle API calls (+ latency, + failure handling) on
  every self-pay checkout.
- **Context:** Paddle has no email filter on subscriptions: `GET /customers?email=<email>` → customer ids,
  then `GET /subscriptions?customer_id=<ids>&status=active,trialing,past_due`. Add
  `PaymentProviderClient.hasLiveSubscription(email)`; `CheckoutService.createCheckout` calls it for
  self-pay only (gifts keep the entitlement check) and returns 409. Fail OPEN if Paddle is unreachable or
  errors, so a Paddle outage can't block legitimate new signups; log at WARN.

### Demo site — seeded, self-resetting public sandbox
A normal TNRA group set up as a public demo: seeded with realistic dummy data and reset on a schedule
(e.g. hourly) so anyone can log in with published credentials and explore a populated app.
- **Why:** Lets prospects experience the real product before committing — supports go-to-market /
  self-serve growth. A populated, always-fresh demo converts far better than screenshots or an empty group.
- **Effort:** M (human: ~2-3 days / CC: ~30 min)
- **Depends on:** Nothing hard — reuses normal provisioning (one regular group). Billing must be OFF or
  the group set billing-exempt so the entitlement gate never blocks the demo login.
- **Context:** Two pieces. (a) A seed dataset — users, posts (intro/kryptonite/commitments/best&worst/
  stats), call chain, a few weeks of history — loadable via SQL or a seed routine. (b) A scheduled reset
  (`@Scheduled` or system cron) that truncates and re-seeds on the chosen interval, so a visitor's edits
  don't persist past the window. Decide cadence (hourly?), publish the login(s), and either disable
  destructive/admin actions for the demo account or accept that the reset cleans them up. Keep the demo
  group's DB/realm/subdomain isolated like any other group.

## Tech debt / DevEx

### ~~Security headers on `tnra-app` (per-group apps)~~ ✓ Completed v10.5.0
Apply the same security response headers to `tnra-app` that `tnra-landing` got in 10.0.1 (HSTS, CSP,
Referrer-Policy, Permissions-Policy) so the per-group subdomains (`<group>.tnra.app`) also score an A
on securityheaders.com.
- **Why:** Only the public landing site is hardened so far; the member-facing per-group apps send no
  HSTS/CSP/Referrer-Policy/Permissions-Policy.
- **Effort:** S (human: ~half day / CC: ~20 min) — but needs care, not a copy-paste.
- **Context:** `tnra-app`'s `SecurityConfig` is OIDC-based, so the CSP must also allow the **Keycloak
  redirect** (`form-action`/`connect-src` for `auth.<domain>`) and **Vaadin push** (websocket via
  `connect-src 'self'`), and the post-payment redirect to the billing/MoR checkout domain. Force HSTS
  the same way (the Cloudflare tunnel hides the HTTPS scheme from the origin). Test the login flow and a
  member session after, then re-scan a group subdomain.

### ~~Revisit `.gitignore` rule for `application.properties`~~ ✓ Completed v10.6.1 (option a)
`.gitignore` ignores `application.properties` (to protect runtime secrets), but every module's
TEST config lives in `src/test/resources/application.properties` and must be `git add -f`'d to commit
(tnra-app and tnra-billing-app both do this). Easy to forget; `git add .` silently skips it.
- **Why:** A required, non-secret, committed file that the default ignore rule fights is a footgun —
  a new test config can be silently left untracked and CI/other devs run without it.
- **Effort:** XS (human: ~30 min / CC: ~10 min)
- **Options:** (a) scope the ignore to main only (`**/src/main/resources/application.properties`) so test
  configs are tracked normally; or (b) rename test config to the Spring-profile form
  `application-test.properties` (hyphen — NOTE: `application.test.properties` with a dot is NOT a Spring
  auto-loaded name) and activate the `test` profile in tests, keeping `application.properties` ignored for
  runtime secrets. Recommend (a) — smallest change, no profile wiring, and test config is genuinely safe to
  commit. Apply consistently across tnra-app + tnra-billing-app (+ any future module).
- **Context:** Touches `.gitignore` and the two committed test `application.properties` files.

### ~~Rename `BILLING_PORT` to `TNRA_BILLING_PORT`~~ ✓ Completed v10.6.1 (old name still read as a fallback)
The billing service's host-port override in `docker-compose.production.yml` is `BILLING_PORT`, while
the landing site's is `TNRA_LANDING_PORT`. Rename it so every host-port override follows the same
`TNRA_<service>_PORT` pattern.
- **Why:** Came up during the 10.4.5 production rollout: 8082 was taken on the production host and the
  override had to be looked up because its name doesn't match the landing one. Consistent naming makes
  `.env` self-explanatory.
- **Effort:** XS (human: ~15 min / CC: ~5 min)
- **Context:** Touches `docker-compose.production.yml` (mapping + comment), `.env.template`, and the
  billing sections of `PRODUCTION.vps.md` / `PRODUCTION.cloudflare.md` / README. Production `.env`
  currently sets `BILLING_PORT=8086`, so keep reading the old name as a fallback
  (`${TNRA_BILLING_PORT:-${BILLING_PORT:-8082}}`) or note the rename in the CHANGELOG so the host
  `.env` gets updated at deploy time. Host port only — the in-network address stays
  `http://tnra-billing:8082`.

### Move production `cloudflared` from the host into Docker
Production (.104) runs `cloudflared` as a host-level systemd service (`cloudflared.service`), so every
tunnel route must target a `127.0.0.1`-published host port (`http://localhost:8081`, `:8180`, `:8086`,
per-group ports). Run it as the compose-managed `cloudflared` container instead, on the `tnra-shared`
network, so routes can use container names (`http://tnra-landing:8080`, `http://keycloak:8080`,
`http://tnra-billing:8082`, `http://tnra-<group>:8080`) and host-port juggling (see the
`BILLING_PORT` rename above) stops mattering.
- **Why:** The runbook, `tnra.start.sh` and `docker-compose.production.yml` all assume the container
  tunnel; the host-level service was a one-off that is now a documented divergence
  (`PRODUCTION.cloudflare.md`, host-level subsection). It also means every tunnel route silently
  depends on a host port that `.env` can change. Found during the 10.4.5 billing rollout, where
  `http://tnra-billing:8082` returned 502 until the route was pointed at `localhost:8086`.
- **Effort:** S (human: ~1 hour, mostly a maintenance window / CC: ~10 min for docs)
- **Context / cutover:** (1) put `CLOUDFLARE_TUNNEL_TOKEN` (the same tunnel's token, from the
  systemd unit's command line) into `~/tnra/.env`; (2) `docker compose -f docker-compose.production.yml
  --profile cloudflare up -d cloudflared` — the dashboard shows two connectors briefly; (3) in Zero Trust
  → Public Hostname, change every route's service to the container form above; (4) `systemctl disable
  --now cloudflared` and remove the unit; (5) verify apex/www, auth, every group subdomain and
  `billing.tnra.app/actuator/health`. From then on `tnra.start.sh` restarts the tunnel on boot because
  the token is in `.env`. Afterwards the host port mappings for landing/billing can go back to defaults
  and the host-level subsection of the runbook can shrink to a one-line note.

### CI: skip the test / production-build jobs for docs-only changes
Every PR runs the full suite (`test`, `cli-test`, `production-build` ≈ 3–5 min of runners) even when
only `*.md`, `TODOS.md`, `CHANGELOG.md` or `.env.template` changed — most of the last twenty PRs were
docs-only.
- **Why:** Wasted runner minutes and a slower merge loop for pure docs; also tempting to skip CI entirely.
- **Effort:** XS (human: ~30 min / CC: ~10 min)
- **Context:** Two gotchas decide the shape. (1) `paths-ignore:` on the workflow triggers is the obvious
  fix but makes the required checks *missing* (not passing) on docs-only PRs, which blocks merge if the
  checks are required — use a "changes" job (`dorny/paths-filter` or `git diff --name-only` against the
  base) and `if: needs.changes.outputs.code == 'true'` on the heavy jobs, with a trailing always-run
  `ci-ok` job that required-status rules point at. (2) The `test` job on `main` also pushes the badges
  and tags the release from `VERSION`; keep `version-check` unconditional and make the tag step run
  from a job that doesn't depend on the suite having run — or keep tagging only on code pushes and
  accept that docs-only bumps (none so far) go untagged. Docs-only = everything under `**/*.md`,
  `TODOS.md`, `CHANGELOG.md`, `.env.template`, `PROVISIONING_PLAN.md`, `DESIGN.md`; anything else counts as code.

## P2 — After MVP Ships

### Completed Post View — Improve Read-Only Contrast
The completed post view renders post fields in a disabled/read-only state that produces low-contrast text, making content hard to read.
- **Why:** Members frequently review past posts; poor legibility undermines the core use case.
- **Effort:** XS (human: ~2 hours / CC: ~10 min)
- **Depends on:** Nothing — purely visual, no data model changes.
- **Context:** Read-only Vaadin `TextArea` and `TextField` components use a muted disabled style by default. Fix by switching to a custom CSS approach (e.g., `pointer-events: none` + explicit text color override via Lumo custom properties or a `.read-only-field` theme variant) so the fields look rendered rather than grayed-out. Evaluate against DESIGN.md before shipping.

### Non-Sequential Primary Keys
Add a `public_id` UUID column to all externally-visible entities (Post, User) so that internal sequential auto-increment PKs are never surfaced in the API, URLs, or notifications.
- **Why:** Sequential integer PKs reveal row count, insertion rate, and allow trivial enumeration. UUID public IDs eliminate all three. Complements the deep-link token work but covers entities beyond just posts (e.g., user profile routes).
- **Effort:** M (human: ~2 days / CC: ~20 min)
- **Depends on:** Deep Link URL Token task shipped (establishes the pattern). No Flyway version conflict at the time this is scheduled.
- **Context:** Add `public_id CHAR(36) NOT NULL DEFAULT (UUID())` columns via a new Flyway migration. Populate existing rows in the same migration. Add a `@Column(unique=true)` `publicId` field to affected JPA entities. Internal foreign keys and JPA relationships keep the numeric PK for performance — only the `publicId` is ever sent over the wire or stored in external systems. Update repositories with `findByPublicId(String)` finders. Audit all Vaadin views and service methods to confirm no numeric ID leaks remain.

### Email Invitation Flow
Send Keycloak registration link when admin invites a member.
- **Why:** Currently admin enters an email and tells the member out-of-band to create a Keycloak account. An email invitation with a registration link is the expected UX.
- **Effort:** S (human: ~1 day / CC: ~15 min)
- **Depends on:** Branch 3 (Keycloak auth) shipped. Keycloak email config in Branch 4.
- **Context:** Keycloak supports sending registration links via its admin API. The invite flow should trigger this. Ties into per-user billing — invitation = start of billing relationship.

### Per-User Billing Integration
Track member count per group for billing. Tie invitation to billing.
- **Why:** Business model is $1-2/member/month. Need to count active members and report to Stripe.
- **Effort:** M (human: ~1 week / CC: ~30 min)
- **Depends on:** Branch 3 (invite flow), Branch 4 (provisioning), Stripe integration.
- **Context:** Each `inviteUser()` call should eventually create a Stripe subscription item. Member deactivation should pause billing. Minimum 4 members per group.

### Meeting Notes Capture
Monthly meeting notes as a rich-text field per month, viewable by all group members.
- **Why:** Currently in Google Docs — consolidates all group accountability data into TNRA.
- **Effort:** S (human: ~2 days / CC: ~30 min)
- **Depends on:** Core wedge (encryption, configurable stats, Keycloak, provisioning) shipped.
- **Context:** Monthly cadence involves a group meeting to review and make new commitments. Notes are currently unstructured in Google Docs. Rich-text editing in Vaadin requires a component (e.g., CKEditor or similar).

### Persistent Session Store (Survive Redeploys)
Move HTTP sessions out of Tomcat's in-memory store into a persistent store (Spring Session + JDBC or Redis) so authenticated sessions survive app restarts/redeploys.
- **Why:** The 30-day "stay logged in" fix (persistent `JSESSIONID` cookie + 30d server timeout, shipped this release) makes sessions survive browser restarts, but server-side sessions are still in-memory — every container restart/redeploy wipes them and forces a (silent, Keycloak-backed) re-auth. For truly uninterrupted 30-day sessions across deploys, sessions must persist outside the JVM.
- **Effort:** M (human: ~2-3 days / CC: ~30 min)
- **Context:** Spring Session JDBC could reuse the existing MySQL (`SPRING_SESSION_*` tables via its schema), avoiding a new Redis dependency — but note session payloads would land in the DB; confirm that's acceptable alongside the app-level column encryption. Per-group deployment means each group's DB carries its own sessions. The silent Keycloak re-auth on restart is low-friction, so this is reliability polish, not urgent. Tackle on the module/parent-pom branch alongside config consolidation. See also [Default the founder-email Notify Property].

### Text/SMS Notifications (Rebuild from Scratch)
Proper SMS notifications using a real provider (Twilio, AWS SNS, or similar).
- **Why:** Current implementation uses brittle carrier email-to-text gateways (e.g., `number@vtext.com`). Unreliable delivery, no confirmation, carrier-dependent. Must be rebuilt properly, not restored.
- **Effort:** M (human: ~1 week / CC: ~30 min)
- **Depends on:** Core wedge shipped, activity-only email notifications pattern established.
- **Context:** If restored, must use activity-only notifications (no post content) to align with encryption-at-rest security posture. Requires a proper SMS API with delivery receipts, not carrier email-to-text. Budget for SMS provider costs ($0.0075/message Twilio).

## P3 — Future Enhancements

### Deactivated User Read-Only Mode
Refine deactivated user behavior: allow login in read-only mode to view own posts, but not other users' posts. No new post creation, no stat updates, no profile changes.
- **Why:** Currently deactivated users are hard-blocked. Read-only access lets departing members retrieve their own history without full app access.
- **Effort:** S (human: ~1 day / CC: ~15 min)
- **Depends on:** Member Deactivation UI (shipped).
- **Context:** Requires a "read-only" session state that restricts Vaadin views. PostView should filter to own posts only, hide "Start New Post", disable form fields. Other views (Admin, Profile, DailyCallsView) should be hidden or redirect.

### Yearly Retreat Prep Format
Structured annual reflection form per member per year, viewable by the group.
- **Why:** The yearly retreat is the anchor event for TNRA groups. A specific proto-format exists in practice but isn't digitized.
- **Effort:** S-M (human: ~3 days / CC: ~30 min)
- **Depends on:** Core wedge shipped, at least one group onboarded.
- **Context:** Recovery and faith groups often build their year around the retreat. Having the prep format built in signals TNRA understands the full cadence. Similar to the weekly post form but for annual reflection.

## Completed

### Fix Placeholder Profile Picture and Uploads Volume in Provisioned Groups
Placeholder profile picture was broken in production-provisioned groups due to two issues: the uploads volume resolved to `~/tnra/uploads/` (shared across groups) instead of `provision/<group-name>/uploads/`, and the bind-mounted directory was owned by root so the unprivileged `appuser` couldn't write to it.
- **Completed:** v8.1.11 — volume path corrected to `./provision/{{GROUP_NAME}}/uploads`, CLI fixed to create `uploads/` without extra sub-directory, `docker-entrypoint.sh` added to chown `/uploads` to `appuser` at startup.

### Unique Constraint on users.email
Add `UNIQUE` constraint via Flyway migration.
- **Completed:** v7.0.0 (2026-03-29) — bundled into V5 migration with personal stats

### Keycloak Logout Should Revoke Keycloak Session
Use `OidcClientInitiatedLogoutSuccessHandler` to call Keycloak's `end_session_endpoint`.
- **Completed:** v7.2.0 (2026-04-02)

### Narrow Keycloak Redirect URI
Change wildcard `/*` to exact match `/login/oauth2/code/keycloak` in realm export.
- **Completed:** v7.2.0 (2026-04-02)

### Move Debug Logging to Dev Profile
Move DEBUG logging for Spring Security/OAuth2/Vaadin to `application-local.yml`.
- **Completed:** v7.2.0 (2026-04-02)

### TaskExecutor Rejection Policy
Change `emailTaskExecutor` from `AbortPolicy` to `CallerRunsPolicy` with graceful shutdown.
- **Completed:** v7.2.0 (2026-04-02)

### Reduce Session Timeout for Production
Reduce from 30 days to 24 hours.
- **Completed:** v7.2.0 (2026-04-02)

### Normalize Notifications
Centralized all notification display in `AppNotification` utility with consistent MIDDLE position, duration, and LUMO theme variants.
- **Completed:** v7.5.1 (2026-04-20)

### Member Deactivation UI
Admin can deactivate/reactivate members from the Members tab. Deactivated users are hard-blocked from app access.
- **Completed:** v7.5.1 (2026-04-20)

### App-Level Column Encryption (AES-256-GCM)
AES-256-GCM encryption for all sensitive post and stat columns. Per-app DEK wrapped with master key in `encryption_keys` table. Transparent JPA converters, Flyway V7+V8 migrations, in-memory name uniqueness checks.
- **Completed:** v8.0.0 (2026-04-24)

### Encrypt `stat_definition.emoji` Column
Encrypts emoji on all stat definitions (global and personal) to prevent metadata leakage. Flyway V9+V10 migrations.
- **Completed:** v8.0.1 (2026-04-24)
