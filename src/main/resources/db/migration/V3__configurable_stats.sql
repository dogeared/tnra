-- V3: Configurable stats — replace embedded Stats columns with dynamic stat_definition + post_stat_value tables

-- 1. Create stat_definition table
CREATE TABLE stat_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    emoji VARCHAR(10),
    stat_type VARCHAR(20) NOT NULL DEFAULT 'NUMERIC',
    display_order INT NOT NULL DEFAULT 0,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_stat_definition_name (name)
);

-- 2. Create post_stat_value table
CREATE TABLE post_stat_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    stat_definition_id BIGINT NOT NULL,
    stat_value INTEGER,
    CONSTRAINT fk_psv_post FOREIGN KEY (post_id) REFERENCES post(id),
    CONSTRAINT fk_psv_stat_def FOREIGN KEY (stat_definition_id) REFERENCES stat_definition(id),
    UNIQUE KEY uk_post_stat (post_id, stat_definition_id)
);

-- 3. Seed the default stat definitions (matching the original 7 hardcoded stats)
INSERT INTO stat_definition (name, label, emoji, stat_type, display_order, archived) VALUES
    ('exercise', 'Exercise', '💪', 'NUMERIC', 0, FALSE),
    ('meditate', 'Meditate', '🧘', 'NUMERIC', 1, FALSE),
    ('pray',     'Pray',     '🙏', 'NUMERIC', 2, FALSE),
    ('read',     'Read',     '📚', 'NUMERIC', 3, FALSE),
    ('gtg',      'GTG',      '👥', 'NUMERIC', 4, FALSE),
    ('meetings', 'Meetings', '🤝', 'NUMERIC', 5, FALSE),
    ('sponsor',  'Sponsor',  '🤲', 'NUMERIC', 6, FALSE);

-- 4. Migrate existing post data from embedded columns to post_stat_value rows
-- For each post that has any stat values, create post_stat_value entries
INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p.exercise
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'exercise' AND p.exercise IS NOT NULL;

INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p.meditate
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'meditate' AND p.meditate IS NOT NULL;

INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p.pray
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'pray' AND p.pray IS NOT NULL;

INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p._read
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'read' AND p._read IS NOT NULL;

INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p.gtg
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'gtg' AND p.gtg IS NOT NULL;

INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p.meetings
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'meetings' AND p.meetings IS NOT NULL;

INSERT INTO post_stat_value (post_id, stat_definition_id, stat_value)
SELECT p.id, sd.id, p.sponsor
FROM post p CROSS JOIN stat_definition sd
WHERE sd.name = 'sponsor' AND p.sponsor IS NOT NULL;

-- 5. Drop the old embedded stat columns from post table
ALTER TABLE post DROP COLUMN exercise;
ALTER TABLE post DROP COLUMN gtg;
ALTER TABLE post DROP COLUMN meditate;
ALTER TABLE post DROP COLUMN meetings;
ALTER TABLE post DROP COLUMN pray;
ALTER TABLE post DROP COLUMN _read;
ALTER TABLE post DROP COLUMN sponsor;
