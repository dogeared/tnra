# Migration Plan: V3 Configurable Stats

**Branch:** branch-2/configurable-stats
**Delete this file after:** Admin stats UI is implemented and production migration is verified.

## Overview

V3 replaces the 7 hardcoded embedded stat columns on the `post` table with a dynamic
`stat_definition` + `post_stat_value` model. All existing stat data is preserved.

### Stats being migrated

| Old Column | New name | Label | Emoji |
|-----------|----------|-------|-------|
| `exercise` | exercise | Exercise | 💪 |
| `meditate` | meditate | Meditate | 🧘 |
| `pray` | pray | Pray | 🙏 |
| `_read` | read | Read | 📚 |
| `gtg` | gtg | GTG | 👥 |
| `meetings` | meetings | Meetings | 🤝 |
| `sponsor` | sponsor | Sponsor | 🤲 |

Posts with non-null values get `post_stat_value` rows. Null stats are not migrated
(absence = "not set" in the new model).

## Step 1: Back up production

```bash
mysqldump -h <host> -u<username> -p \
  --skip-column-statistics --no-tablespaces tnra > ~/tnra-backup-$(date +%Y%m%d).sql
```

Verify:
```bash
ls -la ~/tnra-backup-*.sql
grep "CREATE TABLE" ~/tnra-backup-*.sql | wc -l
grep "INSERT INTO" ~/tnra-backup-*.sql | wc -l
```

## Step 2: Restore to local MySQL

```bash
docker compose up mysql -d
docker compose exec mysql mysqladmin ping -h localhost --wait=30
docker compose exec -T mysql mysql -uroot -p<password> \
  -e "CREATE DATABASE IF NOT EXISTS tnra_migration_test;"
docker compose exec -T mysql mysql -uroot -p<password> tnra_migration_test \
  < ~/tnra-backup-*.sql
```

## Step 3: Verify pre-migration state

```sql
-- Connect: docker compose exec mysql mysql -uroot -p<password> tnra_migration_test

-- Record these numbers for comparison in step 5
SELECT COUNT(*) AS total_posts FROM post;

SELECT
  SUM(exercise IS NOT NULL) AS exercise_count,
  SUM(gtg IS NOT NULL) AS gtg_count,
  SUM(meditate IS NOT NULL) AS meditate_count,
  SUM(meetings IS NOT NULL) AS meetings_count,
  SUM(pray IS NOT NULL) AS pray_count,
  SUM(_read IS NOT NULL) AS read_count,
  SUM(sponsor IS NOT NULL) AS sponsor_count
FROM post;

-- Sample a few posts to verify after migration
SELECT id, exercise, gtg, meditate, meetings, pray, _read, sponsor
FROM post WHERE exercise IS NOT NULL LIMIT 5;

-- Check Flyway state (should have V1 baseline + V2)
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- IMPORTANT: Verify column names match what V3 expects
DESCRIBE post;
-- Must have: exercise, gtg, meditate, meetings, pray, _read, sponsor
-- If _read is named differently, update V3 migration before proceeding
```

## Step 4: Run migration locally

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/tnra_migration_test \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=<password> \
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true \
SPRING_FLYWAY_BASELINE_VERSION=1 \
./mvnw spring-boot:run
```

Watch logs for:
- `Successfully applied 1 migration to schema "tnra_migration_test"`
- `Started TnraApplication`

If it fails: restore from backup (`Step 2`), fix V3 SQL, retry.

## Step 5: Verify data integrity

```sql
-- Connect: docker compose exec mysql mysql -uroot -p<password> tnra_migration_test

-- 1. Stat definitions seeded correctly
SELECT * FROM stat_definition ORDER BY display_order;
-- Expect 7 rows: exercise(0), meditate(1), pray(2), read(3), gtg(4), meetings(5), sponsor(6)

-- 2. Total migrated values
SELECT COUNT(*) AS total_stat_values FROM post_stat_value;
-- Should equal the sum of all non-null counts from step 3

-- 3. Spot-check a known post (use an ID from step 3)
SELECT psv.post_id, sd.name, sd.label, psv.stat_value
FROM post_stat_value psv
JOIN stat_definition sd ON sd.id = psv.stat_definition_id
WHERE psv.post_id = <ID_FROM_STEP_3>
ORDER BY sd.display_order;
-- Values must match what you recorded in step 3

-- 4. Old columns are gone
DESCRIBE post;
-- Must NOT have: exercise, gtg, meditate, meetings, pray, _read, sponsor

-- 5. Flyway history is clean
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
-- V1 (baseline), V2, V3 all with success=1
```

## Step 6: Test the app locally

With the app running against `tnra_migration_test`:

- [ ] Log in and view an old completed post — stats display with correct values
- [ ] View multiple old posts — all stats preserved
- [ ] Start a new post — stat entry form shows all 7 stats
- [ ] Enter stats on the new post — values save correctly
- [ ] Finish the new post — stats persist and display on the completed view

## Step 7: Migrate production

```bash
# On the VPS
cd ~/tnra
git pull origin main

./mvnw clean package -DskipTests -Pproduction
docker compose up --build -d

# Watch logs
docker compose logs -f server
# Look for: "Successfully applied 1 migration" and "Started TnraApplication"
```

## Step 8: Verify production

```sql
-- docker compose exec mysql mysql -uroot -p<password> tnra

SELECT COUNT(*) FROM post_stat_value;
SELECT COUNT(*) FROM stat_definition;
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Then log in and verify posts display correctly.

## Rollback

**If V3 fails mid-migration:** Restore from backup and redeploy Branch 1 code.

```bash
docker compose exec -T mysql mysql -uroot -p<password> tnra < ~/tnra-backup-*.sql
# Redeploy previous version
```

**If V3 succeeds but app is broken:** Restore from backup and redeploy Branch 1 code.
The old stat columns are dropped — there is no partial rollback.

## Known risks

| Risk | Check |
|------|-------|
| `_read` column named differently | `DESCRIBE post` in step 3 |
| Flyway baseline not applied (Branch 1 not deployed first) | `SELECT * FROM flyway_schema_history` — must exist |
| Large dataset slow migration | Unlikely — 7 INSERT...SELECT on hundreds of rows takes <1s |
