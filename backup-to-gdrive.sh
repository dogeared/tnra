#!/bin/bash

# TNRA MySQL Database Backup Script
# Backs up the MySQL database and uploads to Google Drive using a Service Account

set -e

# Debug mode (set via -d flag or DEBUG=1)
DEBUG=${DEBUG:-0}

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/backups"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="tnra_backup_${DATE}.sql"
COMPRESSED_FILE="${BACKUP_FILE}.gz"
GDRIVE_FOLDER_NAME="${GDRIVE_FOLDER_NAME:-tnra-backups}"  # Folder name in Shared Drive
GDRIVE_FOLDER_ID=""  # Will be set dynamically
GDRIVE_SHARED_DRIVE_ID="${GDRIVE_SHARED_DRIVE_ID:-}"  # Shared Drive ID (required)
SERVICE_ACCOUNT_FILE="${SERVICE_ACCOUNT_FILE:-${SCRIPT_DIR}/service-account.json}"

# Database configuration (from docker-compose)
DB_CONTAINER="mysql-prod"
DB_NAME="tnra_the_og"
DB_USER="root"

# Load environment variables
if [ -f "${SCRIPT_DIR}/.env" ]; then
    source "${SCRIPT_DIR}/.env"
fi

DB_PASSWORD="${MYSQL_ROOT_PASSWORD}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    if [ "${DEBUG}" -eq 1 ]; then
        echo -e "${YELLOW}[DEBUG]${NC} $1"
    fi
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if docker is running
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running"
        exit 1
    fi

    # Check if mysql container is running
    if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
        log_error "MySQL container '${DB_CONTAINER}' is not running"
        exit 1
    fi

    # Check if service account file exists
    if [ ! -f "${SERVICE_ACCOUNT_FILE}" ]; then
        log_error "Service account file not found: ${SERVICE_ACCOUNT_FILE}"
        log_error "Please download your service account JSON from Google Cloud Console"
        exit 1
    fi

    # Check if GDRIVE_SHARED_DRIVE_ID is set
    if [ -z "${GDRIVE_SHARED_DRIVE_ID}" ]; then
        log_error "GDRIVE_SHARED_DRIVE_ID is not set"
        log_error "Please create a Shared Drive, add the service account, and set the ID in .env"
        exit 1
    fi

    # Check for required tools
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi

    if ! command -v python3 &> /dev/null; then
        log_error "python3 is required but not installed"
        exit 1
    fi

    log_info "Prerequisites check passed"
}

# Create backup directory
create_backup_dir() {
    if [ ! -d "${BACKUP_DIR}" ]; then
        mkdir -p "${BACKUP_DIR}"
        log_info "Created backup directory: ${BACKUP_DIR}"
    fi
}

# Perform database backup
backup_database() {
    log_info "Starting database backup..."

    docker exec "${DB_CONTAINER}" mysqldump \
        -u"${DB_USER}" \
        -p"${DB_PASSWORD}" \
        --single-transaction \
        --routines \
        --triggers \
        "${DB_NAME}" > "${BACKUP_DIR}/${BACKUP_FILE}"

    if [ $? -eq 0 ]; then
        log_info "Database backup created: ${BACKUP_FILE}"
    else
        log_error "Database backup failed"
        exit 1
    fi
}

# Compress backup
compress_backup() {
    log_info "Compressing backup..."

    gzip -f "${BACKUP_DIR}/${BACKUP_FILE}"

    if [ $? -eq 0 ]; then
        local size=$(du -h "${BACKUP_DIR}/${COMPRESSED_FILE}" | cut -f1)
        log_info "Backup compressed: ${COMPRESSED_FILE} (${size})"
    else
        log_error "Compression failed"
        exit 1
    fi
}

# Get OAuth2 access token using service account
get_access_token() {
    python3 << 'PYTHON_SCRIPT'
import json
import time
import base64
import hashlib
import urllib.request
import urllib.parse
import os
import sys

def base64url_encode(data):
    if isinstance(data, str):
        data = data.encode('utf-8')
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('utf-8')

def create_jwt(service_account_file):
    with open(service_account_file, 'r') as f:
        sa_info = json.load(f)

    # JWT Header
    header = {
        "alg": "RS256",
        "typ": "JWT"
    }

    # JWT Claim set
    now = int(time.time())
    claim_set = {
        "iss": sa_info["client_email"],
        "scope": "https://www.googleapis.com/auth/drive.file",
        "aud": "https://oauth2.googleapis.com/token",
        "iat": now,
        "exp": now + 3600
    }

    # Create the signing input
    signing_input = base64url_encode(json.dumps(header)) + "." + base64url_encode(json.dumps(claim_set))

    # Sign with RSA
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding
    from cryptography.hazmat.backends import default_backend

    private_key = serialization.load_pem_private_key(
        sa_info["private_key"].encode(),
        password=None,
        backend=default_backend()
    )

    signature = private_key.sign(
        signing_input.encode('utf-8'),
        padding.PKCS1v15(),
        hashes.SHA256()
    )

    jwt = signing_input + "." + base64url_encode(signature)
    return jwt

def get_token(service_account_file):
    jwt = create_jwt(service_account_file)

    data = urllib.parse.urlencode({
        'grant_type': 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion': jwt
    }).encode('utf-8')

    req = urllib.request.Request(
        'https://oauth2.googleapis.com/token',
        data=data,
        headers={'Content-Type': 'application/x-www-form-urlencoded'}
    )

    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode('utf-8'))
        return result['access_token']

service_account_file = os.environ.get('SERVICE_ACCOUNT_FILE')
try:
    token = get_token(service_account_file)
    print(token)
except Exception as e:
    print(f"Error getting token: {e}", file=sys.stderr)
    sys.exit(1)
PYTHON_SCRIPT
}

# Find or create the backup folder in Shared Drive
get_or_create_folder() {
    local access_token="$1"

    log_info "Looking for folder '${GDRIVE_FOLDER_NAME}' in Shared Drive..."

    # Search for existing folder by name in the Shared Drive
    local search_query="name='${GDRIVE_FOLDER_NAME}' and mimeType='application/vnd.google-apps.folder' and trashed=false and '${GDRIVE_SHARED_DRIVE_ID}' in parents"
    local encoded_query=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''${search_query}'''))")

    local search_response=$(curl -s -X GET \
        -H "Authorization: Bearer ${access_token}" \
        "https://www.googleapis.com/drive/v3/files?q=${encoded_query}&fields=files(id,name)&supportsAllDrives=true&includeItemsFromAllDrives=true&corpora=drive&driveId=${GDRIVE_SHARED_DRIVE_ID}")

    log_debug "Folder search response: ${search_response}"

    local folder_id=$(echo "${search_response}" | python3 -c "
import sys, json
data = json.load(sys.stdin)
files = data.get('files', [])
if files:
    print(files[0]['id'])
" 2>/dev/null)

    if [ -n "${folder_id}" ]; then
        log_info "Found existing folder: ${GDRIVE_FOLDER_NAME} (${folder_id})"
        GDRIVE_FOLDER_ID="${folder_id}"
        return
    fi

    # Folder doesn't exist, create it in the Shared Drive
    log_info "Folder not found, creating '${GDRIVE_FOLDER_NAME}' in Shared Drive..."

    local create_metadata=$(cat <<EOF
{
    "name": "${GDRIVE_FOLDER_NAME}",
    "mimeType": "application/vnd.google-apps.folder",
    "parents": ["${GDRIVE_SHARED_DRIVE_ID}"]
}
EOF
)

    local create_response=$(curl -s -X POST \
        -H "Authorization: Bearer ${access_token}" \
        -H "Content-Type: application/json" \
        -d "${create_metadata}" \
        "https://www.googleapis.com/drive/v3/files?supportsAllDrives=true")

    log_debug "Folder create response: ${create_response}"

    folder_id=$(echo "${create_response}" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

    if [ -n "${folder_id}" ]; then
        log_info "Created folder: ${GDRIVE_FOLDER_NAME} (${folder_id})"
        GDRIVE_FOLDER_ID="${folder_id}"
    else
        log_error "Failed to create folder. Response: ${create_response}"
        exit 1
    fi
}

# Upload to Google Drive
upload_to_gdrive() {
    log_info "Getting access token..."

    ACCESS_TOKEN=$(SERVICE_ACCOUNT_FILE="${SERVICE_ACCOUNT_FILE}" get_access_token)

    if [ -z "${ACCESS_TOKEN}" ]; then
        log_error "Failed to get access token"
        exit 1
    fi

    # Get or create the backup folder
    get_or_create_folder "${ACCESS_TOKEN}"

    log_info "Uploading to Google Drive..."

    local file_path="${BACKUP_DIR}/${COMPRESSED_FILE}"
    local file_size=$(stat -c%s "${file_path}")

    # Create metadata
    local metadata=$(cat <<EOF
{
    "name": "${COMPRESSED_FILE}",
    "parents": ["${GDRIVE_FOLDER_ID}"]
}
EOF
)

    # Upload using resumable upload for reliability (with Shared Drive support)
    # Step 1: Initiate the upload
    local curl_cmd="curl -s -X POST \
        -H \"Authorization: Bearer ${ACCESS_TOKEN}\" \
        -H \"Content-Type: application/json\" \
        -H \"X-Upload-Content-Type: application/gzip\" \
        -H \"X-Upload-Content-Length: ${file_size}\" \
        -d '${metadata}' \
        \"https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&supportsAllDrives=true\" \
        -D -"

    log_debug "Initiate upload curl command:"
    log_debug "${curl_cmd}"

    local full_response=$(curl -s -X POST \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -H "Content-Type: application/json" \
        -H "X-Upload-Content-Type: application/gzip" \
        -H "X-Upload-Content-Length: ${file_size}" \
        -d "${metadata}" \
        "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&supportsAllDrives=true" \
        -D - 2>&1)

    log_debug "Full response from initiate upload:"
    log_debug "${full_response}"

    local upload_url=$(echo "${full_response}" | grep -i "^location:" | sed 's/location: //i' | tr -d '\r')

    if [ -z "${upload_url}" ]; then
        log_error "Failed to initiate upload"
        if [ "${DEBUG}" -eq 1 ]; then
            log_debug "Response body (if any):"
            log_debug "$(echo "${full_response}" | tail -n 20)"
        fi
        exit 1
    fi

    # Step 2: Upload the file
    local response=$(curl -s -X PUT \
        -H "Content-Length: ${file_size}" \
        -H "Content-Type: application/gzip" \
        --data-binary @"${file_path}" \
        "${upload_url}")

    local file_id=$(echo "${response}" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

    if [ -n "${file_id}" ]; then
        log_info "Upload successful! File ID: ${file_id}"
        log_info "File URL: https://drive.google.com/file/d/${file_id}/view"
    else
        log_error "Upload may have failed. Response: ${response}"
        exit 1
    fi
}

# Cleanup old local backups (keep last 5)
cleanup_local_backups() {
    log_info "Cleaning up old local backups..."

    local backup_count=$(ls -1 "${BACKUP_DIR}"/tnra_backup_*.sql.gz 2>/dev/null | wc -l)

    if [ "${backup_count}" -gt 5 ]; then
        ls -1t "${BACKUP_DIR}"/tnra_backup_*.sql.gz | tail -n +6 | xargs rm -f
        log_info "Removed old backups, keeping last 5"
    fi
}

# Parse command line arguments
parse_args() {
    while getopts "dh" opt; do
        case ${opt} in
            d)
                DEBUG=1
                ;;
            h)
                echo "Usage: $0 [-d] [-h]"
                echo "  -d    Enable debug mode"
                echo "  -h    Show this help message"
                exit 0
                ;;
            *)
                echo "Usage: $0 [-d] [-h]"
                exit 1
                ;;
        esac
    done
}

# Main execution
main() {
    parse_args "$@"

    log_info "=== TNRA Database Backup to Google Drive ==="
    log_info "Started at: $(date)"
    if [ "${DEBUG}" -eq 1 ]; then
        log_debug "Debug mode enabled"
    fi

    check_prerequisites
    create_backup_dir
    backup_database
    compress_backup
    upload_to_gdrive
    cleanup_local_backups

    log_info "=== Backup completed successfully ==="
    log_info "Finished at: $(date)"
}

main "$@"
