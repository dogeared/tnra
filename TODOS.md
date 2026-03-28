# TODOS

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
