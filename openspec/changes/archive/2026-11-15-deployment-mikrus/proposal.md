# Proposal

## Why

The app (REQ-001..052 except the AI summary) is ready and has to run for real: on a phone, over HTTPS, with push.
The user has a Mikrus VPS (1 GB RAM, 10 GB disk) and no own domain yet; Mikrus offers `https://<server>.mikrus.cloud`
with TLS terminated by its proxy. Images must be built for free (public repository: GitHub Actions + ghcr.io).

## What Changes

- Docker images: `backend/Dockerfile` (JRE 25, non-root, small heap) and `frontend/Dockerfile` (Caddy + Angular build).
- `deploy/compose.prod.yml` for 1 GB RAM (memory limits, tuned Postgres, Hikari pool 5), `deploy/Caddyfile`
  (security headers incl. CSP, SPA fallback, caching, `/api` proxy, client IP only from trusted proxies),
  `deploy/backup.sh` (daily `pg_dump`, 14 days), `deploy/.env.example`.
- GitHub Actions `ci.yml`: backend and frontend tests on every PR/push; on `main` builds and pushes both images.
- The inline theme script in `index.html` moves to `theme-init.js` so the CSP needs no `'unsafe-inline'` scripts.
- `docs/deploy.md`: step-by-step commands for the user to paste on the server.
- No behavior change of the app, hence no spec deltas (`skip_specs`).

## Capabilities

### New Capabilities
<!-- none: infrastructure only -->

### Modified Capabilities
<!-- none -->

## Impact

- New files only, plus `index.html`, `.gitignore`, README/CLAUDE.md.
- Server needs Docker; secrets live only in `~/talia/.env` on the server.
