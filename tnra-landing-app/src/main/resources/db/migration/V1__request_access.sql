-- Landing-site schema (tnra-landing-app): the "Request Access" form submissions. Column names must
-- match the RequestAccess entity exactly — the app boots with hibernate.ddl-auto=validate.
-- IF NOT EXISTS: a deploy whose table was created by hand before this migration existed must not fail.

CREATE TABLE IF NOT EXISTS request_access (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    group_name     VARCHAR(255) NOT NULL,
    contact_name   VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    estimated_size INT          NULL,
    description    TEXT         NULL,
    submitted_at   DATETIME     NOT NULL,
    ip_address     VARCHAR(255) NULL,
    PRIMARY KEY (id)
);
