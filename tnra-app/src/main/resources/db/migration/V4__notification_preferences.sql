-- V4: Add per-user notification preference column (defaults to TRUE)

ALTER TABLE users ADD COLUMN notify_new_posts BIT NOT NULL DEFAULT 1;
