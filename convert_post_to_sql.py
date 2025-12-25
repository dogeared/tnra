#!/usr/bin/env python3
"""
Convert a post text/HTML file to SQL INSERT statement.

Usage:
    python convert_post_to_sql.py --user-map users.json <input_file>
    python convert_post_to_sql.py --user-map users.json emails/*.txt > all_posts.sql

The user map JSON file should contain an object mapping email addresses to user IDs:
    {
        "user@example.com": 1,
        "another@example.com": 2
    }

Supports both HTML format (from emails) and plain text format.
"""

import argparse
import html
import json
import re
import sys
from datetime import datetime
from pathlib import Path


def escape_sql(text):
    """Escape single quotes for SQL."""
    if text is None:
        return ""
    return text.replace("'", "''")


def strip_html_tags(text):
    """Remove HTML tags and decode entities."""
    if not text:
        return ""
    # Remove HTML tags
    text = re.sub(r'<[^>]+>', '', text)
    # Decode HTML entities
    text = html.unescape(text)
    # Normalize whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def parse_date(date_str):
    """Parse date from various formats to MySQL datetime format."""
    date_str = date_str.strip()

    # Try different date formats
    formats = [
        "%m/%d/%Y %I:%M:%S %p",  # 12/20/2025 04:29:44 PM
        "%m/%d/%Y %H:%M:%S",      # 12/20/2025 16:29:44
    ]

    for fmt in formats:
        try:
            dt = datetime.strptime(date_str, fmt)
            return dt.strftime("%Y-%m-%d %H:%M:%S")
        except ValueError:
            continue

    print(f"Warning: Could not parse date '{date_str}'", file=sys.stderr)
    return None


def extract_email(content):
    """Extract email address from Post From line."""
    # HTML format: <strong>Post From:</strong> Name - email@example.com
    # Plain text format: Post From: Name - email@example.com
    match = re.search(r'Post From:.*?-\s*([^\s<]+@[^\s<]+)', content, re.IGNORECASE)
    if match:
        return match.group(1).strip()
    return None


def extract_dates(content):
    """Extract start and finish dates from content."""
    # HTML format: <strong>Post Started:</strong> date, <strong>Post Finished:</strong> date
    # Plain text format: Post Started: date, Post Finished: date

    start_match = re.search(r'Post Started:.*?(\d{1,2}/\d{1,2}/\d{4}\s+\d{1,2}:\d{2}:\d{2}\s*(?:AM|PM)?)', content, re.IGNORECASE)
    finish_match = re.search(r'Post Finished:.*?(\d{1,2}/\d{1,2}/\d{4}\s+\d{1,2}:\d{2}:\d{2}\s*(?:AM|PM)?)', content, re.IGNORECASE)

    start_date = parse_date(start_match.group(1)) if start_match else None
    finish_date = parse_date(finish_match.group(1)) if finish_match else None

    return start_date, finish_date


def extract_section_html(content, section_start, section_end):
    """Extract content between HTML section markers."""
    # Build pattern to match content between sections
    # section_start like <h3>WIDWYTK:</h3> or <h3>Best:</h3>
    # section_end like <h3>Kryptonite:</h3> or </h2> etc.

    pattern = rf'{re.escape(section_start)}(.*?){re.escape(section_end)}'
    match = re.search(pattern, content, re.DOTALL | re.IGNORECASE)
    if match:
        return strip_html_tags(match.group(1))
    return ""


def extract_section_between_headers(content, h2_section, h3_subsection, next_h3):
    """Extract content from a specific subsection within an h2 section."""
    # First find the h2 section
    h2_pattern = rf'<h2>{re.escape(h2_section)}</h2>(.*?)(?:<h2>|</body>)'
    h2_match = re.search(h2_pattern, content, re.DOTALL | re.IGNORECASE)
    if not h2_match:
        return ""

    h2_content = h2_match.group(1)

    # Now find the h3 subsection within it
    h3_pattern = rf'<h3>{re.escape(h3_subsection)}</h3>(.*?)(?:<h3>|<h2>|</body>|$)'
    h3_match = re.search(h3_pattern, h2_content, re.DOTALL | re.IGNORECASE)
    if h3_match:
        return strip_html_tags(h3_match.group(1))
    return ""


def extract_stats_html(content):
    """Extract stats from HTML format."""
    stats = {
        'exercise': None,
        'gtg': None,
        'meditate': None,
        'meetings': None,
        'pray': None,
        'read': None,
        'sponsor': None,
    }

    # Find the stats section
    stats_match = re.search(r'<h2>Stats:</h2>(.*?)(?:</body>|$)', content, re.DOTALL | re.IGNORECASE)
    if not stats_match:
        return stats

    stats_content = stats_match.group(1)

    # Extract each stat
    for stat in stats:
        # Match patterns like <strong>exercise:</strong> 3 or exercise: 3
        pattern = rf'(?:<strong>)?{stat}:(?:</strong>)?\s*(\d+)'
        match = re.search(pattern, stats_content, re.IGNORECASE)
        if match:
            stats[stat] = match.group(1)

    return stats


def extract_intro_section(content, section_name, next_section):
    """Extract intro subsection content."""
    # Pattern for <h3>WIDWYTK:</h3><p>content</p>
    pattern = rf'<h3>{re.escape(section_name)}</h3>\s*<p>(.*?)</p>'
    match = re.search(pattern, content, re.DOTALL | re.IGNORECASE)
    if match:
        return strip_html_tags(match.group(1))
    return ""


def parse_plain_text(content):
    """Parse plain text format (like sample_post.txt)."""
    data = {}

    # Extract email
    data['email'] = extract_email(content)

    # Extract dates
    data['start'], data['finish'] = extract_dates(content)

    # Helper to extract section between markers
    def extract_between(start_marker, end_marker):
        pattern = rf'^{re.escape(start_marker)}$\n(.*?)(?=^{re.escape(end_marker)}$|\Z)'
        match = re.search(pattern, content, re.MULTILINE | re.DOTALL)
        if match:
            return match.group(1).strip()
        return ""

    # Extract sections
    data['widwytk'] = extract_between('WIDWYTK:', 'Kryptonite:')
    data['kryptonite'] = extract_between('Kryptonite:', 'What and When:')
    data['what_and_when'] = extract_between('What and When:', 'Personal:')

    # Extract Personal section
    personal_match = re.search(r'^Personal:$\n(.*?)(?=^Family:$|\Z)', content, re.MULTILINE | re.DOTALL)
    if personal_match:
        personal = personal_match.group(1)
        best_match = re.search(r'^Best:$\n(.*?)(?=^Worst:$|\Z)', personal, re.MULTILINE | re.DOTALL)
        worst_match = re.search(r'^Worst:$\n(.*?)(?=^$|\Z)', personal, re.MULTILINE | re.DOTALL)
        data['personal_best'] = best_match.group(1).strip() if best_match else ""
        data['personal_worst'] = worst_match.group(1).strip() if worst_match else ""
    else:
        data['personal_best'] = ""
        data['personal_worst'] = ""

    # Extract Family section
    family_match = re.search(r'^Family:$\n(.*?)(?=^Work:$|\Z)', content, re.MULTILINE | re.DOTALL)
    if family_match:
        family = family_match.group(1)
        best_match = re.search(r'^Best:$\n(.*?)(?=^Worst:$|\Z)', family, re.MULTILINE | re.DOTALL)
        worst_match = re.search(r'^Worst:$\n(.*?)(?=^$|\Z)', family, re.MULTILINE | re.DOTALL)
        data['family_best'] = best_match.group(1).strip() if best_match else ""
        data['family_worst'] = worst_match.group(1).strip() if worst_match else ""
    else:
        data['family_best'] = ""
        data['family_worst'] = ""

    # Extract Work section
    work_match = re.search(r'^Work:$\n(.*?)(?=^Stats:$|\Z)', content, re.MULTILINE | re.DOTALL)
    if work_match:
        work = work_match.group(1)
        best_match = re.search(r'^Best:$\n(.*?)(?=^Worst:$|\Z)', work, re.MULTILINE | re.DOTALL)
        worst_match = re.search(r'^Worst:$\n(.*?)(?=^$|\Z)', work, re.MULTILINE | re.DOTALL)
        data['work_best'] = best_match.group(1).strip() if best_match else ""
        data['work_worst'] = worst_match.group(1).strip() if worst_match else ""
    else:
        data['work_best'] = ""
        data['work_worst'] = ""

    # Extract stats
    stats_line = re.search(r'^exercise:.*$', content, re.MULTILINE)
    if stats_line:
        line = stats_line.group(0)
        data['exercise'] = re.search(r'exercise:\s*(\d+)', line).group(1) if re.search(r'exercise:\s*(\d+)', line) else None
        data['gtg'] = re.search(r'gtg:\s*(\d+)', line).group(1) if re.search(r'gtg:\s*(\d+)', line) else None
        data['meditate'] = re.search(r'meditate:\s*(\d+)', line).group(1) if re.search(r'meditate:\s*(\d+)', line) else None
        data['meetings'] = re.search(r'meetings:\s*(\d+)', line).group(1) if re.search(r'meetings:\s*(\d+)', line) else None
        data['pray'] = re.search(r'pray:\s*(\d+)', line).group(1) if re.search(r'pray:\s*(\d+)', line) else None
        data['read'] = re.search(r'read:\s*(\d+)', line).group(1) if re.search(r'read:\s*(\d+)', line) else None
        data['sponsor'] = re.search(r'sponsor:\s*(\d+)', line).group(1) if re.search(r'sponsor:\s*(\d+)', line) else None

    return data


def parse_html(content):
    """Parse HTML format (from email downloads)."""
    data = {}

    # Extract email
    data['email'] = extract_email(content)

    # Extract dates
    data['start'], data['finish'] = extract_dates(content)

    # Extract intro sections
    data['widwytk'] = extract_intro_section(content, 'WIDWYTK:', 'Kryptonite:')
    data['kryptonite'] = extract_intro_section(content, 'Kryptonite:', 'What and When:')
    data['what_and_when'] = extract_intro_section(content, 'What and When:', 'Personal:')

    # Extract main sections with Best/Worst subsections
    data['personal_best'] = extract_section_between_headers(content, 'Personal:', 'Best:', 'Worst:')
    data['personal_worst'] = extract_section_between_headers(content, 'Personal:', 'Worst:', '')
    data['family_best'] = extract_section_between_headers(content, 'Family:', 'Best:', 'Worst:')
    data['family_worst'] = extract_section_between_headers(content, 'Family:', 'Worst:', '')
    data['work_best'] = extract_section_between_headers(content, 'Work:', 'Best:', 'Worst:')
    data['work_worst'] = extract_section_between_headers(content, 'Work:', 'Worst:', '')

    # Extract stats
    stats = extract_stats_html(content)
    data['exercise'] = stats['exercise']
    data['gtg'] = stats['gtg']
    data['meditate'] = stats['meditate']
    data['meetings'] = stats['meetings']
    data['pray'] = stats['pray']
    data['read'] = stats['read']
    data['sponsor'] = stats['sponsor']

    return data


def parse_file(filepath):
    """Parse a post file (auto-detect format)."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Detect format based on content
    if '<html>' in content.lower() or '<body>' in content.lower():
        return parse_html(content)
    else:
        return parse_plain_text(content)


def generate_sql(data, user_map):
    """Generate SQL INSERT statement from parsed data."""
    email = data.get('email')
    if not email:
        return None, "Could not extract email address"

    user_id = user_map.get(email)
    if not user_id:
        return None, f"No user_id mapping for email: {email}"

    # Format values
    def sql_str(val):
        if val is None or val == "":
            return "NULL"
        return f"'{escape_sql(val)}'"

    def sql_int(val):
        if val is None or val == "":
            return "NULL"
        return f"'{val}'"

    sql = f"""INSERT INTO `post` (`family_best`, `family_worst`, `finish`, `kryptonite`, `what_and_when`, `widwytk`, `personal_best`, `personal_worst`, `start`, `state`, `exercise`, `gtg`, `meditate`, `meetings`, `pray`, `reade`, `sponsor`, `work_best`, `work_worst`, `user_id`, `_read`)
VALUES
\t({sql_str(data.get('family_best'))}, {sql_str(data.get('family_worst'))}, '{data.get('finish')}', {sql_str(data.get('kryptonite'))}, {sql_str(data.get('what_and_when'))}, {sql_str(data.get('widwytk'))}, {sql_str(data.get('personal_best'))}, {sql_str(data.get('personal_worst'))}, '{data.get('start')}', 'complete', {sql_int(data.get('exercise'))}, {sql_int(data.get('gtg'))}, {sql_int(data.get('meditate'))}, {sql_int(data.get('meetings'))}, {sql_int(data.get('pray'))}, {sql_int(data.get('read'))}, {sql_int(data.get('sponsor'))}, {sql_str(data.get('work_best'))}, {sql_str(data.get('work_worst'))}, '{user_id}', NULL);"""

    return sql, None


def load_user_map(json_path):
    """Load user mappings from a JSON file."""
    path = Path(json_path)
    if not path.exists():
        print(f"Error: User map file not found: {json_path}", file=sys.stderr)
        sys.exit(1)

    try:
        with open(path, 'r', encoding='utf-8') as f:
            user_map = json.load(f)
        if not isinstance(user_map, dict):
            print(f"Error: User map must be a JSON object (dict)", file=sys.stderr)
            sys.exit(1)
        return user_map
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in user map file: {e}", file=sys.stderr)
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description='Convert post text/HTML files to SQL INSERT statements'
    )
    parser.add_argument(
        '--user-map',
        required=True,
        help='Path to JSON file containing email to user_id mappings'
    )
    parser.add_argument(
        'files',
        nargs='+',
        help='Input file(s) to convert'
    )

    args = parser.parse_args()

    # Load user mappings from JSON file
    user_map = load_user_map(args.user_map)

    errors = []

    for filepath in args.files:
        path = Path(filepath)
        if not path.exists():
            errors.append(f"File not found: {filepath}")
            continue

        try:
            data = parse_file(path)
            sql, error = generate_sql(data, user_map)

            if error:
                errors.append(f"{filepath}: {error}")
            elif sql:
                print(sql)
                print()  # Blank line between statements
        except Exception as e:
            errors.append(f"{filepath}: {str(e)}")

    # Print errors to stderr
    if errors:
        print("\n-- Errors:", file=sys.stderr)
        for error in errors:
            print(f"-- {error}", file=sys.stderr)
        sys.exit(1 if len(errors) == len(args.files) else 0)


if __name__ == '__main__':
    main()
