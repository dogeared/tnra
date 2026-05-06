-- Encryption key storage: one row per database (container-per-tenant model)
CREATE TABLE encryption_keys (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    encrypted_key TEXT     NOT NULL,
    created_at DATETIME    NOT NULL,
    PRIMARY KEY (id)
);

-- stat_definition.name: widen for ciphertext, drop DB-level uniqueness (app enforces in-memory)
ALTER TABLE stat_definition DROP INDEX uq_stat_name_user;
ALTER TABLE stat_definition MODIFY COLUMN name TEXT NOT NULL;

-- stat_definition.label: widen for ciphertext
ALTER TABLE stat_definition MODIFY COLUMN label TEXT NOT NULL;

-- post_stat_value.stat_value: INT -> TEXT to hold AES-256-GCM ciphertext
ALTER TABLE post_stat_value MODIFY COLUMN stat_value TEXT;
