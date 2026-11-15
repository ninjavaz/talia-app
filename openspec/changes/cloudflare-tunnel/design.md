# Design

## Context

- Today: browser -> Cloudflare -> Mikrus gateway (`2a01:4f9:c013:f5c9::1`) -> Caddy on port 80 (host network) ->
  backend on `127.0.0.1:8080`. Caddy trusts `X-Forwarded-For` only from `TRUSTED_PROXIES` (the gateway's /64).
- The Mikrus wiki recommends the same realip approach for nginx and lists `2a01:04f9::/32` with a question mark as
  "the pool Mikrus uses", i.e. the gateway address is not guaranteed.

## Decisions (to confirm when implementing)

- **Tunnel managed in the Cloudflare dashboard** (token-based `cloudflared tunnel run --token ...`): no credentials
  file on the server, the public hostname -> `http://localhost:80` mapping lives in Cloudflare.
- **`cloudflared` in the host network** (like Caddy) so `localhost:80` is Caddy; Caddy binds `127.0.0.1:80` and
  `[::1]:80` only, so nothing listens publicly.
- **Client IP**: trust only loopback; Caddy reads `Cf-Connecting-Ip` (set by Cloudflare, passed by `cloudflared`)
  or `X-Forwarded-For` - pick one when implementing and verify that a forged header from the browser is overwritten.
- **Keep `*.mikrus.cloud` working during the switch?** Optional: until DNS is switched, the old path can stay; after
  the switch, close it (Caddy no longer on the public port).

## Risks / Trade-offs

- [Cloudflare outage] -> the app is unreachable (same today, the Mikrus gateway is behind Cloudflare too).
- [Push subscriptions bound to the old origin] -> users re-enable notifications once (documented).
