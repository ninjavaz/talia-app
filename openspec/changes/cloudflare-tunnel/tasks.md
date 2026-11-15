# Tasks

> Start only after the domain is bought and added to Cloudflare.

## 1. Preparation (user)

- [ ] 1.1 Buy `mytalia.app` (check the renewal price), add it to Cloudflare (free plan), create a tunnel in Zero Trust -> Networks -> Tunnels, copy its token; verify the tunnel shows up in the dashboard

## 2. Stack

- [ ] 2.1 `cloudflared` service (host network, `CLOUDFLARE_TUNNEL_TOKEN`), Caddy on loopback only, `TRUSTED_PROXIES` loopback, client IP header from the tunnel; verify locally that the app works through a tunnel and a forged client IP header is ignored
- [ ] 2.2 `.env.example`, `docs/deploy.md` (new section replacing step 6 and 11), CLAUDE.md; verify the docs steps on the server

## 3. Switch

- [ ] 3.1 Public hostname `mytalia.app` -> `http://localhost:80` in the tunnel, `PUBLIC_URL` and `SITE_ADDRESS` updated, `./update.sh`; verify login, push (re-enable on the phone) and the client IP in the Caddy log
- [ ] 3.2 Close the public port 80 (Caddy on loopback only) and archive this change
