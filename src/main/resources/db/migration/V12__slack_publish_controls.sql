-- V12: Slack publishing controls — group-level toggles and per-user opt-ins.
-- All default to FALSE so existing groups/users keep today's "activity-only" Slack behavior.

ALTER TABLE group_settings
    ADD COLUMN slack_publish_post_data BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN slack_publish_stats     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN slack_publish_post_body BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN slack_publish_stats     BIT NOT NULL DEFAULT 0,
    ADD COLUMN slack_publish_post_body BIT NOT NULL DEFAULT 0;
