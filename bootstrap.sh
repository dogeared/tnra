#!/usr/bin/env bash
# bootstrap.sh — Interactive production setup for TNRA shared infrastructure
# Sets up .env, starts MySQL and Keycloak (and optionally cloudflared).
set -euo pipefail

# ---------------------------------------------------------------------------
# Color helpers
# ---------------------------------------------------------------------------
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { printf "${CYAN}[INFO]${RESET}  %s\n" "$*"; }
success() { printf "${GREEN}[OK]${RESET}    %s\n" "$*"; }
warn()    { printf "${YELLOW}[WARN]${RESET}  %s\n" "$*"; }
die()     { printf "${RED}[ERROR]${RESET} %s\n" "$*" >&2; exit 1; }

# Read a secret without echo
prompt_secret() {
  local prompt="$1"
  local var
  printf "%s" "$prompt" >&2
  read -r -s var < /dev/tty
  printf "\n" >&2
  printf "%s" "$var"
}

# ---------------------------------------------------------------------------
# Banner
# ---------------------------------------------------------------------------
printf "\n"
printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════╗${RESET}\n"
printf "${BOLD}${CYAN}║          TNRA Production Bootstrap                       ║${RESET}\n"
printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════╝${RESET}\n"
printf "\n"
info "This script sets up the shared production infrastructure:"
info "  • Writes a .env file with all required secrets"
info "  • Starts MySQL and Keycloak via docker-compose.production.yml"
info "  • Optionally starts cloudflared (Cloudflare Tunnel mode)"
printf "\n"

# ---------------------------------------------------------------------------
# Check prerequisites
# ---------------------------------------------------------------------------
info "Checking prerequisites..."

for cmd in docker openssl; do
  if ! command -v "$cmd" &>/dev/null; then
    die "'$cmd' is not installed or not in PATH. Please install it first."
  fi
done

# 'docker compose' (v2 plugin) vs 'docker-compose' (v1 standalone)
if docker compose version &>/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose &>/dev/null; then
  COMPOSE="docker-compose"
else
  die "Neither 'docker compose' nor 'docker-compose' found. Please install Docker Compose."
fi

success "All prerequisites satisfied (using: $COMPOSE)"
printf "\n"

# ---------------------------------------------------------------------------
# Guard against existing .env
# ---------------------------------------------------------------------------
if [[ -f .env ]]; then
  warn ".env already exists."
  printf "  Overwrite it? [y/N] " >&2
  read -r OVERWRITE < /dev/tty
  if [[ ! "$OVERWRITE" =~ ^[Yy]$ ]]; then
    info "Keeping existing .env. Exiting."
    exit 0
  fi
  printf "\n"
fi

# ---------------------------------------------------------------------------
# Gather values interactively
# ---------------------------------------------------------------------------
printf "${BOLD}Configuration${RESET}\n"
printf "─────────────────────────────────────────────────────────────\n"

# Domain (required)
while true; do
  printf "Domain (e.g. tnra.app) [required]: " >&2
  read -r DOMAIN < /dev/tty
  if [[ -n "$DOMAIN" ]]; then
    break
  fi
  warn "Domain is required."
done

KEYCLOAK_HOSTNAME="https://auth.${DOMAIN}"

# MySQL root password
MYSQL_ROOT_PASSWORD_LABEL="(generated)"
_mysql_pw=$(prompt_secret "MySQL root password [blank = auto-generate]: ")
if [[ -z "$_mysql_pw" ]]; then
  MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32)
else
  MYSQL_ROOT_PASSWORD="$_mysql_pw"
  MYSQL_ROOT_PASSWORD_LABEL="(set)"
fi

# Keycloak admin username
printf "Keycloak admin username [admin]: " >&2
read -r _kc_user < /dev/tty
KEYCLOAK_ADMIN="${_kc_user:-admin}"

# Keycloak admin password
KEYCLOAK_ADMIN_PASSWORD_LABEL="(generated)"
_kc_pw=$(prompt_secret "Keycloak admin password [blank = auto-generate]: ")
if [[ -z "$_kc_pw" ]]; then
  KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -base64 32)
else
  KEYCLOAK_ADMIN_PASSWORD="$_kc_pw"
  KEYCLOAK_ADMIN_PASSWORD_LABEL="(set)"
fi

# Encryption master key
printf "\n"
warn "WARNING: The encryption master key must NEVER change after first startup."
warn "         Rotating it without re-encrypting the DEK will make all stored"
warn "         post content permanently unreadable. Store it in a secrets vault."
printf "\n"
ENCRYPTION_KEY_LABEL="(generated)"
_enc_key=$(prompt_secret "Encryption master key [blank = auto-generate]: ")
if [[ -z "$_enc_key" ]]; then
  TNRA_ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32)
else
  TNRA_ENCRYPTION_MASTER_KEY="$_enc_key"
  ENCRYPTION_KEY_LABEL="(set)"
fi

# Cloudflare Tunnel token (optional)
printf "\n"
info "Cloudflare Tunnel token — leave blank to use VPS/Nginx mode."
_cf_token=$(prompt_secret "Cloudflare Tunnel token [blank = skip]: ")
CLOUDFLARE_TUNNEL_TOKEN="${_cf_token:-}"
if [[ -n "$CLOUDFLARE_TUNNEL_TOKEN" ]]; then
  CF_LABEL="(set)"
  USE_CLOUDFLARE=true
else
  CF_LABEL="(skipped — VPS/Nginx mode)"
  USE_CLOUDFLARE=false
fi

# ---------------------------------------------------------------------------
# Summary table
# ---------------------------------------------------------------------------
printf "\n"
printf "${BOLD}Summary${RESET}\n"
printf "─────────────────────────────────────────────────────────────\n"
printf "  %-35s %s\n" "Domain:"                  "$DOMAIN"
printf "  %-35s %s\n" "Keycloak hostname:"        "$KEYCLOAK_HOSTNAME"
printf "  %-35s %s\n" "MySQL root password:"      "$MYSQL_ROOT_PASSWORD_LABEL"
printf "  %-35s %s\n" "Keycloak admin username:"  "$KEYCLOAK_ADMIN"
printf "  %-35s %s\n" "Keycloak admin password:"  "$KEYCLOAK_ADMIN_PASSWORD_LABEL"
printf "  %-35s %s\n" "Encryption master key:"    "$ENCRYPTION_KEY_LABEL"
printf "  %-35s %s\n" "Cloudflare Tunnel token:"  "$CF_LABEL"
printf "\n"

# ---------------------------------------------------------------------------
# Confirm
# ---------------------------------------------------------------------------
printf "Proceed? [Y/n] " >&2
read -r CONFIRM < /dev/tty
if [[ "$CONFIRM" =~ ^[Nn]$ ]]; then
  info "Aborted."
  exit 0
fi
printf "\n"

# ---------------------------------------------------------------------------
# Write .env
# ---------------------------------------------------------------------------
info "Writing .env..."

cat > .env <<ENVEOF
# TNRA production environment — generated by bootstrap.sh
# Keep this file secret. Never commit it to version control.

# --- Infrastructure ---
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD
# Cloudflare Tunnels only — leave blank for VPS/Nginx setup
CLOUDFLARE_TUNNEL_TOKEN=$CLOUDFLARE_TUNNEL_TOKEN

# --- Keycloak ---
KEYCLOAK_HOSTNAME=$KEYCLOAK_HOSTNAME
# Set to https://auth.<domain> in production
KEYCLOAK_ADMIN=$KEYCLOAK_ADMIN
KEYCLOAK_ADMIN_PASSWORD=$KEYCLOAK_ADMIN_PASSWORD
# Per-group values — filled in after provisioning each group
KEYCLOAK_CLIENT_ID=
KEYCLOAK_CLIENT_SECRET=
KEYCLOAK_ISSUER_URI=

# --- App (filled in after provisioning each group) ---
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
SPRING_DATASOURCE_URL=
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=1
VAADIN_PRODUCTIONMODE=true
TNRA_NOTIFY_SCHEDULE=

# --- Encryption ---
# WARNING: set once before first startup and never change — rotating this key
# without re-encrypting all data will make all stored post content unreadable.
TNRA_ENCRYPTION_MASTER_KEY=$TNRA_ENCRYPTION_MASTER_KEY

# --- Email ---
MAILGUN_KEY_PRIVATE=
MAILGUN_KEY_PUBLIC=
MAILGUN_URL=

# --- Monitoring ---
DD_AGENT_MAJOR_VERSION=
DD_API_KEY=
DD_SITE=

# --- Logging ---
LOGGING_LEVEL_COM_AFITNERD_TNRA=
LOGGING_LEVEL_ORG_APACHE_HTTP=
LOGGING_LEVEL_ORG_APACHE_HTTP_WIRE=
ENVEOF

success ".env written."
printf "\n"

# ---------------------------------------------------------------------------
# Start services
# ---------------------------------------------------------------------------
info "Starting shared infrastructure..."

if [[ "$USE_CLOUDFLARE" == true ]]; then
  info "Starting mysql, keycloak, and cloudflared (--profile cloudflare)..."
  $COMPOSE -f docker-compose.production.yml --profile cloudflare up -d mysql keycloak cloudflared
else
  info "Starting mysql and keycloak (VPS/Nginx mode)..."
  $COMPOSE -f docker-compose.production.yml up -d mysql keycloak
fi

# ---------------------------------------------------------------------------
# Wait for MySQL
# ---------------------------------------------------------------------------
info "Waiting for MySQL to be ready (timeout: 60s)..."
TIMEOUT=60
ELAPSED=0
until $COMPOSE -f docker-compose.production.yml exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
  if [[ $ELAPSED -ge $TIMEOUT ]]; then
    die "MySQL did not become healthy within ${TIMEOUT}s. Check: $COMPOSE -f docker-compose.production.yml logs mysql"
  fi
  sleep 2
  ELAPSED=$((ELAPSED + 2))
done
success "MySQL is ready."
printf "\n"

# ---------------------------------------------------------------------------
# Credentials table — show all generated secrets in plaintext
# ---------------------------------------------------------------------------
printf "${BOLD}${GREEN}Bootstrap complete! Save these credentials now.${RESET}\n"
printf "─────────────────────────────────────────────────────────────\n"
printf "  %-35s %s\n" "Domain:"                  "$DOMAIN"
printf "  %-35s %s\n" "Keycloak admin URL:"       "$KEYCLOAK_HOSTNAME/admin"
printf "  %-35s %s\n" "Keycloak admin username:"  "$KEYCLOAK_ADMIN"
printf "  %-35s %s\n" "Keycloak admin password:"  "$KEYCLOAK_ADMIN_PASSWORD"
printf "  %-35s %s\n" "MySQL root password:"      "$MYSQL_ROOT_PASSWORD"
printf "  %-35s %s\n" "Encryption master key:"    "$TNRA_ENCRYPTION_MASTER_KEY"
if [[ -n "$CLOUDFLARE_TUNNEL_TOKEN" ]]; then
  printf "  %-35s %s\n" "Cloudflare Tunnel token:"  "$CLOUDFLARE_TUNNEL_TOKEN"
fi
printf "\n"
warn "Store these values in a password manager or secrets vault before proceeding."
printf "\n"

# ---------------------------------------------------------------------------
# Next steps
# ---------------------------------------------------------------------------
printf "${BOLD}Next Steps${RESET}\n"
printf "─────────────────────────────────────────────────────────────\n"
printf "\n"
printf "  ${BOLD}Step 1${RESET} — Build the CLI (run on your local machine):\n"
printf "\n"
printf "    cd cli && mvn package -DskipTests && cd ..\n"
printf "\n"
printf "  ${BOLD}Step 2${RESET} — Provision your first group:\n"
printf "\n"
printf "    java -jar cli/target/tnra-cli.jar provision <group-name> \\\\\n"
printf "      --domain %s \\\\\n" "$DOMAIN"
printf "      --admin-email <email> \\\\\n"
printf "      --admin-first-name <first> \\\\\n"
printf "      --admin-last-name <last>\n"
printf "\n"
printf "  ${BOLD}Step 3${RESET} — Follow the provisioning guide:\n"
if [[ "$USE_CLOUDFLARE" == true ]]; then
  printf "    See PRODUCTION.cloudflare.md → Provisioning New Groups\n"
else
  printf "    See PRODUCTION.vps.md → Provisioning New Groups\n"
fi
printf "\n"
