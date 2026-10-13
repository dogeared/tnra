#!/usr/bin/env bash
# Seed the DEV group app (`tnra` database on the dockerized dev MySQL) with the four demo members that
# ship in the dev Keycloak realm, plus their default profile images.
#
# Why: Keycloak authenticates the demo accounts, but the app shows "Not a Member" until they exist in
# its own `users` table (production groups get this from the CLI's seed-admin.sql). A fresh database
# (`docker compose down -v`) has no rows. Safe to re-run: existing rows are re-activated and re-pointed
# at the default images.
#
# Usage (from the repo root, with the dev infra up and the group app started at least once so Flyway
# has created the schema):
#   ./seed-dev.sh
set -euo pipefail
cd "$(dirname "$0")"

IMAGES_SRC="tnra-cli-app/src/main/resources/templates"   # jerry.png, kramer.png, elaine.png, george.png
UPLOAD_DIR="uploads"                                      # app.file-storage.upload-dir, relative to the app's cwd (repo root)

MYSQL_ROOT_PASSWORD="$(grep -E '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2-)"
[ -n "$MYSQL_ROOT_PASSWORD" ] || { echo "MYSQL_ROOT_PASSWORD is not set in .env" >&2; exit 1; }

mkdir -p "$UPLOAD_DIR"
for who in jerry kramer elaine george; do
  cp "$IMAGES_SRC/$who.png" "$UPLOAD_DIR/$who.png"
done
echo "Copied default profile images to $UPLOAD_DIR/"

docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" 2>&1 <<'SQL' | grep -v 'Using a password'
INSERT INTO tnra.users (email, first_name, last_name, active, profile_image) VALUES
  ('jerry@seinfeld.com',  'Jerry',  'Seinfeld', 1, 'jerry.png'),
  ('kramer@seinfeld.com', 'Cosmo',  'Kramer',   1, 'kramer.png'),
  ('elaine@seinfeld.com', 'Elaine', 'Benes',    1, 'elaine.png'),
  ('george@seinfeld.com', 'George', 'Costanza', 1, 'george.png')
ON DUPLICATE KEY UPDATE active = 1, profile_image = VALUES(profile_image);
SELECT id, email, first_name, last_name, profile_image FROM tnra.users ORDER BY id;
SQL
echo "Seeded demo members into tnra.users"
