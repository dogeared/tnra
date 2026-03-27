-- Make slack columns nullable (were NOT NULL, now optional)
ALTER TABLE users MODIFY COLUMN slack_username VARCHAR(255) NULL;
ALTER TABLE users MODIFY COLUMN slack_user_id VARCHAR(255) NULL;

-- Drop PQ token columns (feature removed)
ALTER TABLE users DROP COLUMN pq_access_token;
ALTER TABLE users DROP COLUMN pq_refresh_token;
