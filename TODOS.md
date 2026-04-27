# TODOS

## P1.5 — Branch 5 (Next Up)

### [NEXT] Deploy Encryption to Active Group
Roll out v8.0.3 to the current active group for real-world testing before new features land.
- **Why:** Validates encryption migrations, JPA converters, and master key config in production before the user base grows.
- **Effort:** XS (human: ~1 hour / CC: n/a — ops task)
- **Context:** Run Flyway V7–V10 migrations against the live DB. Confirm encrypted columns read/write correctly via the UI. Master key must be set in the group's `.env`. This is a gate before Slack Part 1 ships.

### Slack Integration — Part 1: Activity-Only with Post Link
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

### Landing Page with Request Access Form
Static or Vaadin public route for prospective groups. Form: group name, contact name, email, estimated size, description. Submissions stored in `request_access` table + email notification to founder.
- **Why:** Need somewhere to point prospective groups. Supports go-to-market.
- **Effort:** S (human: ~2 days / CC: ~15 min)
- **Depends on:** Slack Part 1 shipped.
- **Context:** Requires Spring Security rule for anonymous access without exposing other routes. Rate limiting on form (max 5/hour per IP).

### Slack Integration — Part 2: Stats and Full-Post Tiers
Extend Part 1 with two additional admin-selectable content tiers: stats-only (username + stat values, no narrative text) and full-post (all post sections, decrypted at send time). Admin selects tier per group.
- **Why:** Groups that are comfortable with the trade-off can opt into richer Slack notifications.
- **Effort:** S-M (human: ~2 days / CC: ~20 min)
- **Depends on:** Slack Part 1 shipped, landing page shipped, at least one group providing feedback on Part 1.
- **Context:** Tier selection stored in `group_settings`. Full-post tier decrypts content in-memory before sending to Slack — clear security warning in the admin UI that post content will leave the encrypted DB. Stats-only tier sends stat names + values only (no narrative). Slack message layout adapts per tier.

## P2 — After MVP Ships

### Temporary Password Change Not Enforced on First Login
When provisioning creates an admin user with a temporary password in Keycloak, the user is not prompted to change it on first login.
- **Why:** Temporary passwords are a security control — if the user is never forced to change it, the provisioned credential stays active indefinitely.
- **Effort:** XS (human: ~30 min / CC: ~10 min)
- **Context:** Likely a Keycloak realm config issue. Keycloak has a `requiredActions: ["UPDATE_PASSWORD"]` on the user account that should trigger a change-password flow on first login. Check whether this is set in `realm.json.tmpl` for the admin user and whether the Keycloak client/realm has the required action enabled. May also need to verify the OIDC flow surfaces the required action challenge correctly.




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
