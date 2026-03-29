-- Personal stats: add discriminator column and user FK
ALTER TABLE stat_definition ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE stat_definition ADD COLUMN user_id BIGINT NULL;
ALTER TABLE stat_definition ADD CONSTRAINT fk_stat_def_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Replace simple unique on name with compound unique on (name, user_id)
-- Note: MySQL allows duplicate NULLs, so global stat name uniqueness is app-enforced
ALTER TABLE stat_definition DROP INDEX `name`;
ALTER TABLE stat_definition ADD UNIQUE INDEX uq_stat_name_user (name, user_id);

-- Email unique constraint (from TODOS P0)
ALTER TABLE users ADD UNIQUE INDEX uq_user_email (email);
