#!/bin/sh
# Dumps the database every night at 03:00 UTC into /backups (talia-YYYY-MM-DD.sql.gz) and keeps 14 days.
# The dumps contain all personal data: copy them off the server now and then, keep them private.
set -eu
while true; do
  # sleep until the next 03:00 UTC (not right at start: the schema may not exist yet)
  now=$(date -u +%s)
  sleep $(( (86400 + 3 * 3600 - now % 86400 - 1) % 86400 + 1 ))

  file="/backups/talia-$(date -u +%F).sql.gz"
  if pg_dump --no-owner | gzip > "$file.tmp"; then
    mv "$file.tmp" "$file"
    echo "backup written: $file"
  else
    rm -f "$file.tmp"
    echo "backup FAILED" >&2
  fi
  find /backups -name 'talia-*.sql.gz' -mtime +14 -delete
done
