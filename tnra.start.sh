#! /bin/bash
# Brings up the full TNRA production stack: shared infrastructure + every provisioned group.
# Started by tnra.service on boot.

cd ~/tnra

# Export everything in .env so BILLING_ADMIN_TOKEN (and friends) are visible here and available for
# docker-compose variable interpolation.
set -a
source ./.env
set +a

# The shared Docker network and the tunnel/Keycloak are dogeared-central-infra's (its
# central-infra.service runs before this one). Wait for the network rather than fail the boot.
for i in $(seq 1 60); do
  docker network inspect tnra-shared >/dev/null 2>&1 && break
  sleep 2
done

# Shared infrastructure (mysql, tnra-landing), plus the central billing service when a billing admin
# token is set — an opt-in compose profile that a bare `up` would skip, so without this a reboot would
# bring the stack back WITHOUT it.
PROFILES=()
if [ -n "${BILLING_ADMIN_TOKEN:-}" ]; then
  PROFILES+=(--profile billing)
fi
docker compose -f docker-compose.production.yml "${PROFILES[@]}" up -d

# Every provisioned group site under provision/.
for g in provision/*/; do
  [ -f "${g}docker-compose.yml" ] || continue
  docker compose -f docker-compose.production.yml -f "${g}docker-compose.yml" up -d
done
