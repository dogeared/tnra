#!/usr/bin/env python3
"""
Download emails from Gmail filtered by sender.

Usage:
    python download_emails.py --start-date 2025-01-01 --end-date 2025-12-31 --output-dir ./emails --sender me@example.com

Prerequisites:
    1. pip install google-auth google-auth-oauthlib google-api-python-client
    2. Create a Google Cloud project and enable Gmail API
    3. Create OAuth 2.0 credentials (Desktop app) and download as credentials.json
    4. Place credentials.json in the same directory as this script
"""

import argparse
import base64
import os
import re
import sys
from datetime import datetime
from pathlib import Path

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build

# Gmail API scope for reading emails
SCOPES = ['https://www.googleapis.com/auth/gmail.readonly']


def get_gmail_service():
    """Authenticate and return Gmail API service."""
    creds = None
    script_dir = Path(__file__).parent
    token_path = script_dir / 'token.json'
    credentials_path = script_dir / 'credentials.json'

    # Load existing token if available
    if token_path.exists():
        creds = Credentials.from_authorized_user_file(str(token_path), SCOPES)

    # If no valid credentials, authenticate
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            if not credentials_path.exists():
                print(f"Error: {credentials_path} not found.")
                print("\nTo set up Gmail API access:")
                print("1. Go to https://console.cloud.google.com/")
                print("2. Create a new project (or select existing)")
                print("3. Enable the Gmail API")
                print("4. Create OAuth 2.0 credentials (Desktop app)")
                print("5. Download the credentials and save as 'credentials.json'")
                sys.exit(1)

            flow = InstalledAppFlow.from_client_secrets_file(str(credentials_path), SCOPES)
            creds = flow.run_local_server(port=0)

        # Save credentials for future runs
        with open(token_path, 'w') as token:
            token.write(creds.to_json())

    return build('gmail', 'v1', credentials=creds)


def parse_date(date_str):
    """Parse date string in YYYY-MM-DD format."""
    try:
        return datetime.strptime(date_str, '%Y-%m-%d')
    except ValueError:
        print(f"Error: Invalid date format '{date_str}'. Use YYYY-MM-DD.")
        sys.exit(1)


def sanitize_filename(subject, msg_id, date_str):
    """Create a safe filename from email subject and date."""
    # Remove or replace unsafe characters
    safe_subject = re.sub(r'[<>:"/\\|?*]', '_', subject or 'no_subject')
    safe_subject = safe_subject[:50]  # Limit length
    # Format: YYYY-MM-DD_subject_msgid.txt
    return f"{date_str}_{safe_subject}_{msg_id[:8]}.txt"


def get_email_body(payload):
    """Extract plain text body from email payload."""
    body = ""

    if 'body' in payload and payload['body'].get('data'):
        body = base64.urlsafe_b64decode(payload['body']['data']).decode('utf-8', errors='replace')
    elif 'parts' in payload:
        for part in payload['parts']:
            mime_type = part.get('mimeType', '')
            if mime_type == 'text/plain':
                if part['body'].get('data'):
                    body = base64.urlsafe_b64decode(part['body']['data']).decode('utf-8', errors='replace')
                    break
            elif mime_type.startswith('multipart/'):
                # Recursively check nested parts
                body = get_email_body(part)
                if body:
                    break

    return body


def download_emails(start_date, end_date, output_dir, sender_email):
    """Download emails from Gmail matching criteria."""
    service = get_gmail_service()

    # Create output directory if it doesn't exist
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    # Build Gmail search query
    # Gmail uses after/before with epoch seconds or YYYY/MM/DD format
    start_str = start_date.strftime('%Y/%m/%d')
    end_str = end_date.strftime('%Y/%m/%d')
    query = f'from:{sender_email} after:{start_str} before:{end_str}'

    print(f"Searching for emails from {sender_email}")
    print(f"Date range: {start_date.date()} to {end_date.date()}")
    print(f"Output directory: {output_path.absolute()}")
    print(f"Gmail query: {query}")
    print("-" * 50)

    # Fetch message IDs matching the query
    messages = []
    page_token = None

    while True:
        results = service.users().messages().list(
            userId='me',
            q=query,
            pageToken=page_token
        ).execute()

        if 'messages' in results:
            messages.extend(results['messages'])

        page_token = results.get('nextPageToken')
        if not page_token:
            break

    print(f"Found {len(messages)} emails matching criteria")

    if not messages:
        print("No emails to download.")
        return

    # Download each email
    downloaded = 0
    for i, msg_info in enumerate(messages, 1):
        msg_id = msg_info['id']

        # Fetch full message
        msg = service.users().messages().get(
            userId='me',
            id=msg_id,
            format='full'
        ).execute()

        # Extract headers
        headers = {h['name']: h['value'] for h in msg['payload'].get('headers', [])}
        subject = headers.get('Subject', 'No Subject')
        from_addr = headers.get('From', '')
        date_header = headers.get('Date', '')

        # Parse date for filename
        try:
            # Handle various date formats
            for fmt in ['%a, %d %b %Y %H:%M:%S %z', '%d %b %Y %H:%M:%S %z']:
                try:
                    email_date = datetime.strptime(date_header.split(' (')[0].strip(), fmt)
                    date_for_filename = email_date.strftime('%Y-%m-%d')
                    break
                except ValueError:
                    continue
            else:
                date_for_filename = 'unknown-date'
        except Exception:
            date_for_filename = 'unknown-date'

        # Extract body
        body = get_email_body(msg['payload'])

        # Create filename and save
        filename = sanitize_filename(subject, msg_id, date_for_filename)
        filepath = output_path / filename

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(body)

        print(f"[{i}/{len(messages)}] Saved: {filename}")
        downloaded += 1

    print("-" * 50)
    print(f"Downloaded {downloaded} emails to {output_path.absolute()}")


def main():
    parser = argparse.ArgumentParser(
        description='Download emails from Gmail filtered by sender'
    )
    parser.add_argument(
        '--start-date',
        required=True,
        help='Start date in YYYY-MM-DD format'
    )
    parser.add_argument(
        '--end-date',
        required=True,
        help='End date in YYYY-MM-DD format'
    )
    parser.add_argument(
        '--output-dir',
        required=True,
        help='Directory to save downloaded emails'
    )
    parser.add_argument(
        '--sender',
        required=True,
        help='Email address of sender to filter on'
    )

    args = parser.parse_args()

    start_date = parse_date(args.start_date)
    end_date = parse_date(args.end_date)

    if start_date > end_date:
        print("Error: Start date must be before end date.")
        sys.exit(1)

    download_emails(start_date, end_date, args.output_dir, args.sender)


if __name__ == '__main__':
    main()
