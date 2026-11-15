#!/bin/sh
# Updates MyTalia on the server (run it in the app directory, e.g. /apps/talia):
#   ./update.sh               newest version (images tagged "latest")
#   ./update.sh <commit-sha>  that version (rollback / pin); stored as TAG in .env
#   ./update.sh latest        back to the newest version after a pin
#
# Steps: refresh the deployment files from GitHub (main), back up the database, pull the images, restart,
# wait until the backend is healthy, remove old images. .env is never touched except for TAG.
set -eu
cd "$(dirname "$0")"

# TALIA_RAW: another source of the files (tests only)
RAW="${TALIA_RAW:-https://raw.githubusercontent.com/ninjavaz/talia-app/main/deploy}"
COMPOSE="docker compose -f compose.prod.yml"

say() { printf '\n==> %s\n' "$*"; }
fail() { printf '\nERROR: %s\n' "$*" >&2; exit 1; }

[ -f .env ] || fail ".env is missing in $(pwd) (see docs/deploy.md, step 4)"

# 1. version
if [ $# -ge 1 ]; then
  case "$1" in
    latest | [0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]*) ;;
    *) fail "version must be 'latest' or a commit SHA, got: $1" ;;
  esac
  if grep -q '^TAG=' .env; then
    sed -i "s/^TAG=.*/TAG=$1/" .env
  else
    printf 'TAG=%s\n' "$1" >> .env
  fi
fi
TAG=$(sed -n 's/^TAG=//p' .env | tail -n 1)
say "Version: ${TAG:-latest}"

# 2. deployment files (a changed update.sh restarts itself)
say "Deployment files"
for f in update.sh compose.prod.yml backup.sh; do
  curl -fsSL "$RAW/$f" -o "$f.new" || { rm -f "$f.new"; fail "download of $f failed"; }
  if [ -f "$f" ] && cmp -s "$f" "$f.new"; then
    rm "$f.new"
    echo "  $f unchanged"
  else
    mv "$f.new" "$f"
    echo "  $f updated"
    if [ "$f" = update.sh ]; then
      chmod +x update.sh
      echo "  restarting the new update.sh"
      exec ./update.sh
    fi
  fi
done

# 3. backup before the update (only when the database is running)
if $COMPOSE ps --status running --services 2>/dev/null | grep -qx db; then
  say "Database backup"
  mkdir -p backups
  file="backups/before-update-$(date -u +%Y-%m-%d-%H%M).sql.gz"
  $COMPOSE exec -T db pg_dump -U talia --no-owner talia | gzip > "$file.tmp" || { rm -f "$file.tmp"; fail "backup failed, nothing was changed"; }
  mv "$file.tmp" "$file"
  echo "  $file ($(du -h "$file" | cut -f1))"
  find backups -name 'before-update-*.sql.gz' -mtime +30 -delete
fi

# 4. images and restart
say "Pulling images"
$COMPOSE pull
say "Restarting"
$COMPOSE up -d --remove-orphans

# 5. wait for the backend (database migrations run at its start)
say "Waiting for the backend"
i=0
while :; do
  id=$($COMPOSE ps -q backend)
  status=$(docker inspect -f '{{.State.Health.Status}}' "$id" 2>/dev/null || echo unknown)
  [ "$status" = healthy ] && break
  i=$((i + 1))
  if [ "$i" -gt 60 ]; then
    $COMPOSE logs --tail 60 backend
    fail "backend is not healthy after 3 minutes (status: $status). Roll back with ./update.sh <previous sha>"
  fi
  sleep 3
done
echo "  backend healthy"

# 6. clean up
docker image prune -f > /dev/null
say "Done"
$COMPOSE ps --format 'table {{.Service}}\t{{.Status}}'
