#! /bin/bash
# Stops the full TNRA production stack: every provisioned group + shared infrastructure.

cd ~/tnra

# Stop each provisioned group site.
for g in provision/*/; do
  [ -f "${g}docker-compose.yml" ] || continue
  docker compose -f docker-compose.production.yml -f "${g}docker-compose.yml" stop
done

# Stop shared infrastructure (include the tunnel profile so cloudflared stops too).
docker compose -f docker-compose.production.yml --profile cloudflare stop
