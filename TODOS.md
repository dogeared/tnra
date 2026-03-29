# TODOS

## P0 — Security / Correctness

### Keycloak Logout Should Revoke Keycloak Session
Current logout only clears the Spring session. Keycloak session stays alive, so clicking "Login" silently re-authenticates. Use `OidcClientInitiatedLogoutSuccessHandler` to call Keycloak's `end_session_endpoint`.
- **Effort:** S (human: ~1 hr / CC: ~10 min)
- **Depends on:** Branch 3 shipped.

### Narrow Keycloak Redirect URI
Change `redirectUris: ["http://localhost:8080/*"]` in realm export to exact match `["http://localhost:8080/login/oauth2/code/keycloak"]`. Wildcard enables open-redirect attacks.
- **Effort:** S (human: ~15 min / CC: ~5 min)

### Move Debug Logging to Dev Profile
Default `application.yml` has DEBUG logging for Spring Security and OAuth2. In production this logs tokens, headers, and PII. Move to a `dev` or `local` profile.
- **Effort:** S (human: ~30 min / CC: ~5 min)

### TaskExecutor Rejection Policy
`emailTaskExecutor` uses default `AbortPolicy` — if queue fills (50 tasks), emails are silently dropped. Change to `CallerRunsPolicy` for backpressure. Add `setWaitForTasksToCompleteOnShutdown(true)`.
- **Effort:** S (human: ~15 min / CC: ~5 min)

### Reduce Session Timeout for Production
30-day server-side session (`server.servlet.session.timeout: 30d`) is too long for a multi-user app with sensitive content. Reduce to 8-24 hours for production.
- **Effort:** S (human: ~15 min / CC: ~5 min)

## P1 — Next Up

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

### Member Deactivation UI
Admin can deactivate/remove members from the Members tab.
- **Why:** Admin needs to manage membership lifecycle — members leave groups, billing needs to reflect.
- **Effort:** S (human: ~1 day / CC: ~15 min)
- **Depends on:** Branch 3 shipped.
- **Context:** Set `user.active = false`, which already excludes them from email notifications and active user queries. Don't delete — preserve post history.

## P2 — After MVP Ships

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
