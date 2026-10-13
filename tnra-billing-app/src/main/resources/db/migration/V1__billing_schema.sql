-- Central billing schema (tnra-billing-app). One instance shared by every group; the source of truth
-- for entitlement. Column names must match the JPA entities exactly — the service boots with
-- hibernate.ddl-auto=validate, so any drift fails startup rather than silently diverging.
--
-- The provider-specific columns keep their original ls_* names (Lemon Squeezy era); they now hold the
-- Paddle customer / subscription / price-variant identifiers. See BillingAccount for the mapping.

CREATE TABLE group_billing (
    group_slug     VARCHAR(64)  NOT NULL,
    exempt         BOOLEAN      NOT NULL DEFAULT FALSE,   -- forever-free pilot group
    comp_until     DATETIME     NULL,                     -- whole-group trial / promo end
    api_token_hash VARCHAR(128) NOT NULL,                 -- per-group bearer token (hashed)
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_slug)
);

CREATE TABLE billing_account (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    group_slug         VARCHAR(64)  NOT NULL,
    email              VARCHAR(255) NOT NULL,             -- beneficiary (who is entitled)
    payer_email        VARCHAR(255) NULL,                 -- NULL = self-pay; set = gifted by this person
    status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING_PAYMENT',
    exempt             BOOLEAN      NOT NULL DEFAULT FALSE,
    comp_until         DATETIME     NULL,
    ls_customer_id     VARCHAR(64)  NULL,
    ls_subscription_id VARCHAR(64)  NULL,
    ls_variant         VARCHAR(16)  NULL,                 -- 'monthly' | 'yearly'
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_group_email (group_slug, email),
    KEY idx_subscription (ls_subscription_id),
    KEY idx_customer (ls_customer_id),
    KEY idx_payer (group_slug, payer_email)                -- "subscriptions I'm covering"
);

CREATE TABLE billing_webhook_event (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    ls_event_id        VARCHAR(128)  NOT NULL,            -- provider event id; idempotency guard
    event_name         VARCHAR(64)   NOT NULL,
    raw_payload        LONGTEXT      NOT NULL,            -- full body for replay / reconciliation
    matched_account_id BIGINT        NULL,                -- NULL = unmatched, needs reconciliation
    processed          BOOLEAN       NOT NULL DEFAULT FALSE,
    error              VARCHAR(1000) NULL,
    received_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ls_event_id (ls_event_id)
);
