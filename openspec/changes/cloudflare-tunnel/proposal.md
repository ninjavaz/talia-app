# Proposal

> Status: **planned**, to be implemented after the domain (`mytalia.app`) is bought and added to Cloudflare.

## Why

Behind the free `*.mikrus.cloud` address the app trusts the Mikrus HTTP gateway (`2a01:4f9:c013:f5c9::/64`,
measured on 2026-11-15) to pass the client IP in `X-Forwarded-For`. Mikrus does not document that address, so it may
change (the app then keeps working with stricter, shared rate limits), and port 80 has to stay open on the server's
public IPv6. A Cloudflare Tunnel (described in the Mikrus wiki: "Podpięcie domeny przez tunel CloudFlare") removes
both: the server opens an outgoing connection to Cloudflare, no inbound port is needed, and the client IP arrives
only through the tunnel from localhost - no list of proxy addresses to maintain.

## What Changes

- New `cloudflared` service in `deploy/compose.prod.yml` (token from `.env`, `CLOUDFLARE_TUNNEL_TOKEN`), forwarding
  `https://mytalia.app` to Caddy on `127.0.0.1:80`.
- Caddy listens on `127.0.0.1:80` only (no public port); `TRUSTED_PROXIES` = `127.0.0.1/32 ::1/128`, client IP from
  `Cf-Connecting-Ip` / `X-Forwarded-For` set by `cloudflared`.
- `PUBLIC_URL=https://mytalia.app`; HSTS stays; `.app` is HSTS-preloaded anyway.
- `docs/deploy.md`: buying the domain, adding it to Cloudflare (free plan), creating the tunnel in Zero Trust,
  token into `.env`, switching DNS, verifying the client IP in the log; section 6 (Mikrus proxy) becomes obsolete.
- Push: devices subscribed on the old address enable notifications again on the new one (different origin).

## Capabilities

### New Capabilities
<!-- none: infrastructure only -->

### Modified Capabilities
<!-- none -->

## Impact

- `deploy/compose.prod.yml`, `deploy/Caddyfile`, `deploy/.env.example`, `docs/deploy.md`, CLAUDE.md.
- Memory: `cloudflared` needs about 20-40 MB (fits the 1 GB server).
- Data path: Cloudflare terminates TLS (it can see the traffic) - accepted, as with the current Mikrus gateway,
  which is also behind Cloudflare.
