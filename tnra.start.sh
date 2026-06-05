#! /bin/bash
# Brings up the full TNRA production stack: shared infrastructure + every provisioned group.
# Started by tnra.service on boot.

cd ~/tnra

# Export everything in .env so CLOUDFLARE_TUNNEL_TOKEN (and friends) are visible here and
# available for docker-compose variable interpolation.
set -a
source ./.env
set +a

# Shared infrastructure (mysql, keycloak, tnra-landing). Add the Cloudflare tunnel when a
# token is present; otherwise this is VPS/Nginx mode.
if [ -n "${CLOUDFLARE_TUNNEL_TOKEN:-}" ]; then
  docker compose -f docker-compose.production.yml --profile cloudflare up -d
else
  docker compose -f docker-compose.production.yml up -d
fi

# Every provisioned group site under provision/.
for g in provision/*/; do
  [ -f "${g}docker-compose.yml" ] || continue
  docker compose -f docker-compose.production.yml -f "${g}docker-compose.yml" up -d
done
