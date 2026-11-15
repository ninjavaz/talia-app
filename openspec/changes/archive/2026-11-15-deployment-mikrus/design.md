# Design

## Context

- Mikrus: 1 GB RAM, 10 GB disk, public IPv6; `https://<server>.mikrus.cloud` -> port 80 over IPv6, TLS by Mikrus.
- Backend reads `DB_*`, `PUBLIC_URL`, `MAIL_*`, `ALTCHA_HMAC_SECRET`, `VAPID_*` from the environment and trusts
  `X-Forwarded-*` (`server.forward-headers-strategy: framework`); rate limits are per client IP.
- The service worker and push need HTTPS and a production build.

## Decisions

- **Two images** (backend, web) built in CI, pulled by the server: no builds on a 1 GB machine. Tags `latest` and the
  commit SHA (rollback by `TAG=`). Public ghcr.io packages: free; no secrets inside.
- **Caddy instead of nginx**: one config for both cases - plain `:80` behind the Mikrus proxy now, automatic Let's
  Encrypt for an own domain later (`SITE_ADDRESS`).
- **Client IP**: Caddy trusts `X-Forwarded-For` only from `TRUSTED_PROXIES` (`trusted_proxies_strict`, read from the
  right) and sends the backend exactly one address (`header_up X-Forwarded-For {client_ip}`), so the first value the
  backend reads cannot be spoofed. The Mikrus proxy address is read from the log after the first start (docs step 6).
- **CSP**: `script-src 'self'` (theme script moved out of `index.html`), `style-src 'self' 'unsafe-inline'` (Angular
  component styles), `worker-src 'self' blob:` (ALTCHA worker), `connect-src 'self'`, `frame-ancestors 'none'`.
- **Memory**: JVM `-Xmx220m`, SerialGC, limited metaspace/code cache; Postgres `shared_buffers=48MB`,
  `max_connections=20`; container limits db 200 MB, backend 420 MB, web 64 MB, backup 64 MB.
- **Backups**: a `postgres` container dumping once a day (no host cron needed), 14 days kept on the server.
- **Mail**: provider-agnostic SMTP as before; until it is set up, accounts are confirmed by SQL (documented).

## Risks / Trade-offs

- [1 GB is tight] -> limits per container; if the backend gets OOM-killed, raise its limit or add RAM.
- [Mikrus proxy IP unknown until deployed] -> until `TRUSTED_PROXIES` is set, all users share the proxy's IP for the
  rate limits (fail-safe: limits are stricter, not looser).
- [CI cannot run the Playwright e2e cheaply] -> e2e stays local; CI runs unit and integration tests.

## Assumptions and findings during implementation

- Verified locally with the production stack; three issues found and fixed: an empty `MAIL_FROM` in compose overrode
  the default (backend did not start), Angular's critical CSS inlining (Beasties) added an inline script blocked by
  the CSP (disabled), and the first backup ran before the schema existed (now nightly at 03:00 UTC).
- `backend/mvnw` lacked the executable bit in git (repository created on Windows); fixed for CI and image builds.
- GitGuardian flagged `POSTGRES_PASSWORD: ${DB_PASSWORD:?...}` as a generic password: a false positive (only a variable
  reference); the line was simplified, the incident on the old commit is to be marked as false positive.
- The server architecture is assumed to be x86_64 (images are built for linux/amd64); docs step 1 checks it.
