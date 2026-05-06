-- Hibernate 6 defaults enums to STRING (varchar) instead of ORDINAL (tinyint).
-- Convert existing tinyint values to their string equivalents.
ALTER TABLE post MODIFY COLUMN state VARCHAR(255);

UPDATE post SET state = 'IN_PROGRESS' WHERE state = '0';
UPDATE post SET state = 'COMPLETE' WHERE state = '1';
