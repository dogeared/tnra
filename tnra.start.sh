#! /bin/bash
# Brings up the full TNRA production stack: shared infrastructure + every provisioned group.
# Started by tnra.service on boot.

cd ~/tnra

# Export everything in .env so CLOUDFLARE_TUNNEL_TOKEN (and friends) are visible here and
# available for docker-compose variable interpolation.
set -a
source ./.env
set +a

# Shared infrastructure (mysql, tnra-landing; Keycloak lives in the dogeared/keycloak project, which
# starts after this one — see its keycloak.service). Add the Cloudflare tunnel when a
# token is present (otherwise this is VPS/Nginx mode), and the central billing service when a
# billing admin token is set — both are opt-in compose profiles that a bare `up` would skip,
# so without this a reboot would bring the stack back WITHOUT them.
PROFILES=()
if [ -n "${CLOUDFLARE_TUNNEL_TOKEN:-}" ]; then
  PROFILES+=(--profile cloudflare)
fi
if [ -n "${BILLING_ADMIN_TOKEN:-}" ]; then
  PROFILES+=(--profile billing)
fi
docker compose -f docker-compose.production.yml "${PROFILES[@]}" up -d

# Every provisioned group site under provision/.
for g in provision/*/; do
  [ -f "${g}docker-compose.yml" ] || continue
  docker compose -f docker-compose.production.yml -f "${g}docker-compose.yml" up -d
done
