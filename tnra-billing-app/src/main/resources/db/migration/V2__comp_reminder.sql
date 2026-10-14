-- V2: "free period ending" reminder bookkeeping.
-- comp_reminder_for records the coverage-end date the member was last reminded about, so each
-- distinct end date (group trial end, a later member waiver) gets exactly one email.
ALTER TABLE billing_account
    ADD COLUMN comp_reminder_for DATETIME NULL;
