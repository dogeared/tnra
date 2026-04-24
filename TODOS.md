# TODOS

## P1.5 — Branch 5 (Landing Page + Encryption)


### Landing Page with Request Access Form
Static or Vaadin public route for prospective groups. Form: group name, contact name, email, estimated size, description. Submissions stored in `request_access` table + email notification to founder.
- **Why:** Need somewhere to point prospective groups. Supports go-to-market.
- **Effort:** S (human: ~2 days / CC: ~15 min)
- **Depends on:** Branch 4 shipped.
- **Context:** Requires Spring Security rule for anonymous access without exposing other routes. Rate limiting on form (max 5/hour per IP).

## P2 — After MVP Ships

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

### Slack Integration (Optional Per-Group)
Re-introduce Slack integration as an opt-in feature for groups that use Slack.
- **Why:** Removed from MVP to simplify provisioning. Not every group uses Slack (faith/recovery groups often don't).
- **Effort:** M (human: ~1 week / CC: ~30 min)
- **Depends on:** Core wedge shipped, at least one group onboarded.
- **Context:** Current implementation has slash commands, broadcast channel, and API service. Per-group Slack app provisioning would need to be added to the CLI. Activity-only notifications (no post content) to align with encryption-at-rest security posture.

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
- **Why:** The yearly retreat is the anchor event for TNRA groups. A specific prep format exists in practice but isn't digitized.
- **Effort:** S-M (human: ~3 days / CC: ~30 min)
- **Depends on:** Core wedge shipped, at least one group onboarded.
- **Context:** Recovery and faith groups often build their year around the retreat. Having the prep format built in signals TNRA understands the full cadence. Similar to the weekly post form but for annual reflection.

## Completed

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
