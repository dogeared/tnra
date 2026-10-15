#!/usr/bin/env bash
# Bump the project version everywhere it lives, in one go.
#
#   ./bump-version.sh patch            10.6.6 -> 10.6.7
#   ./bump-version.sh minor            10.6.6 -> 10.7.0
#   ./bump-version.sh major            10.6.6 -> 11.0.0
#   ./bump-version.sh 10.8.2           explicit
#
# What it touches (CI's version-check job requires all of these to agree):
#   - every pom.xml, via the versions-maven-plugin (the same `versions:set` used for every release)
#   - VERSION (the file CI reads to tag main)
#   - CHANGELOG.md: a new "## [x.y.z] - <today>" section at the top, with a TODO line to replace
#     (skip with --no-changelog)
#
# It does not commit. Review `git diff`, write the changelog entry, then commit.
set -euo pipefail

cd "$(dirname "$0")"

usage() { sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-1}"; }

CHANGELOG=1
BUMP=""
for arg in "$@"; do
  case "$arg" in
    --no-changelog) CHANGELOG=0 ;;
    -h|--help) usage 0 ;;
    -*) echo "Unknown option: $arg" >&2; usage ;;
    *) BUMP="$arg" ;;
  esac
done
[ -n "$BUMP" ] || usage

CURRENT=$(tr -d '[:space:]' < VERSION)
IFS=. read -r MAJOR MINOR PATCH <<< "$CURRENT"
case "$BUMP" in
  patch) NEXT="$MAJOR.$MINOR.$((PATCH + 1))" ;;
  minor) NEXT="$MAJOR.$((MINOR + 1)).0" ;;
  major) NEXT="$((MAJOR + 1)).0.0" ;;
  *)
    if [[ "$BUMP" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then NEXT="$BUMP"
    else echo "Expected patch | minor | major | x.y.z, got: $BUMP" >&2; usage; fi ;;
esac

echo "Bumping $CURRENT -> $NEXT"

# Every module shares the project version; -DprocessAllModules sets each pom's own <version>.
./mvnw -q versions:set -DnewVersion="$NEXT" -DgenerateBackupPoms=false -DprocessAllModules

printf '%s\n' "$NEXT" > VERSION

if [ "$CHANGELOG" = 1 ]; then
  TODAY=$(date +%Y-%m-%d)
  if grep -q "^## \[$NEXT\]" CHANGELOG.md; then
    echo "CHANGELOG.md already has a [$NEXT] section — leaving it alone."
  else
    # Insert the new section above the first existing "## [" heading.
    awk -v v="$NEXT" -v d="$TODAY" '
      !done && /^## \[/ { print "## [" v "] - " d; print ""; print "### Changed"; print "- TODO: describe this release."; print ""; done=1 }
      { print }
    ' CHANGELOG.md > CHANGELOG.md.tmp && mv CHANGELOG.md.tmp CHANGELOG.md
  fi
fi

# Sanity: everything CI compares must now agree.
MISMATCH=0
for p in pom.xml */pom.xml; do
  V=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$p" | head -1)
  [ "$V" = "$NEXT" ] || { echo "!! $p says $V" >&2; MISMATCH=1; }
done
[ "$MISMATCH" = 0 ] || exit 1

echo
echo "Updated: VERSION, $(ls pom.xml */pom.xml | wc -l | tr -d ' ') pom.xml files$( [ "$CHANGELOG" = 1 ] && echo ', CHANGELOG.md')"
[ "$CHANGELOG" = 1 ] && echo "Next: replace the TODO line in CHANGELOG.md, then commit."
