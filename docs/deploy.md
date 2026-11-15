# Deployment on a Mikrus VPS

Talia runs as four containers from `deploy/compose.prod.yml`: `db` (Postgres), `backend` (Spring Boot), `web`
(Caddy with the Angular app, proxies `/api`) and `backup` (daily database dump). GitHub Actions builds the images on
every push to `main` and publishes them to `ghcr.io/ninjavaz/talia-backend` and `ghcr.io/ninjavaz/talia-web`; the
server only pulls them.

HTTPS: Mikrus forwards `https://<server>.mikrus.cloud` to port 80 of the server (over IPv6) and handles the
certificate. Caddy therefore serves plain HTTP on port 80. With an own domain later, Caddy gets the certificate itself
(see the last section).

Every command below runs on the server over SSH unless it says "on your computer". Replace `<server>` with the
server name from the Mikrus panel (the part before `.mikrus.cloud`).

## 1. Check the server (read-only)

```bash
uname -m                      # must be x86_64 (the images are built for linux/amd64)
free -m                       # 1 GB is enough, the stack uses about 600-700 MB
df -h /                       # the images and the database need about 2 GB
docker --version && docker compose version
```

If `docker` is missing, install it first (Mikrus: `curl -fsSL https://get.docker.com | sh`, or the installer from the
Mikrus panel).

## 2. Make the images public (once, on GitHub)

After the first successful run of the `CI` workflow on `main`, open GitHub -> your profile -> Packages ->
`talia-backend` -> Package settings -> Change visibility -> Public. Same for `talia-web`. Public packages are free and
the server can pull them without a login. The images contain no secrets.

## 3. Get the deployment files

```bash
mkdir -p /apps/talia && cd /apps/talia
base=https://raw.githubusercontent.com/ninjavaz/talia-app/main/deploy
curl -fsSLO "$base/compose.prod.yml"
curl -fsSLO "$base/backup.sh"
curl -fsSL "$base/.env.example" -o .env
chmod 600 .env
mkdir -p backups
```

## 4. Fill in `.env`

Generate the secrets on the server:

```bash
echo "DB_PASSWORD=$(openssl rand -hex 32)"
echo "ALTCHA_HMAC_SECRET=$(openssl rand -hex 48)"
```

On your computer, in the repository: `scripts\vapid-keys.cmd` prints `VAPID_PUBLIC_KEY` and `VAPID_PRIVATE_KEY`.

Then edit the file (`nano .env`) and set at least:

| Variable | Value |
|---|---|
| `PUBLIC_URL` | `https://<server>.mikrus.cloud` |
| `DB_PASSWORD`, `ALTCHA_HMAC_SECRET` | the generated values |
| `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` | from `vapid-keys.cmd` |
| `VAPID_SUBJECT` | `mailto:` + your e-mail address |
| `MAIL_*` | SMTP of an e-mail provider (see section 7); can stay empty at first |

Keep the other values. Never change `DB_PASSWORD` after the first start (the database keeps the first one).

## 5. Start

```bash
cd /apps/talia
docker compose -f compose.prod.yml pull
docker compose -f compose.prod.yml up -d
docker compose -f compose.prod.yml ps          # all services "running" / "healthy" after about a minute
docker compose -f compose.prod.yml logs backend --tail 50
```

Open `https://<server>.mikrus.cloud`. The first start creates the database schema (Flyway).

## 6. Client IP behind the Mikrus proxy

The backend limits failed logins and registrations per client IP. Behind the Mikrus proxy Caddy must know the proxy's
address to read the real client IP from `X-Forwarded-For`. Open the app once in the browser, then:

```bash
docker compose -f compose.prod.yml logs web --tail 20
```

Each request line shows `remote_ip` (who connected to Caddy: the Mikrus proxy) and `client_ip`. Put the proxy
address(es) into `.env`, e.g. `TRUSTED_PROXIES=2a01:4f9:xxxx::1/128` (several separated by spaces), then:

```bash
docker compose -f compose.prod.yml up -d web
```

Open the app again: `client_ip` in the log must now be your own address (compare with https://ifconfig.co on your
phone or computer), not the proxy's.

## 7. E-mails

Registration needs a confirmed e-mail address. Any SMTP provider works (e.g. Brevo or Resend, both with a free tier):
set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, add the provider's SPF/DKIM records to
the sender domain, and restart: `docker compose -f compose.prod.yml up -d backend`.

Until then an account can be confirmed by hand after registering:

```bash
docker compose -f compose.prod.yml exec db \
  psql -U talia -c "UPDATE app_user SET email_verified_at = now() WHERE username = 'your-login';"
```

## 8. iPhone

Open `https://<server>.mikrus.cloud` in Safari -> Share -> "Add to Home Screen". Start the app from the new icon, log
in, then Settings -> Notifications -> "Turn on for this device" and "Send a test". Push works only in the app started
from the home screen (iOS 16.4+).

## 9. Updates and rollback

Every merge to `main` publishes new images (tagged `latest` and with the commit SHA). To update:

```bash
cd /apps/talia
docker compose -f compose.prod.yml pull
docker compose -f compose.prod.yml up -d
docker image prune -f                          # free disk space (old images)
```

Rollback: set `TAG=<commit sha>` in `.env` and run the two commands again. Database migrations only go forward, so
roll back only to a version with the same migrations, or restore a backup.

When `compose.prod.yml` or `backup.sh` change in the repository, download them again (section 3, without `.env`).

## 10. Backups

`backup` writes `/apps/talia/backups/talia-YYYY-MM-DD.sql.gz` every night at 03:00 UTC and keeps 14 days. A backup
right now (e.g. before an update):

```bash
docker compose -f compose.prod.yml exec backup sh -c 'pg_dump --no-owner | gzip > /backups/manual-$(date -u +%F-%H%M).sql.gz'
```

 Copy them off the server now
and then (on your computer: `scp -P <ssh-port> root@<server-host>:/apps/talia/backups/*.sql.gz .`); they contain all
personal data, keep them private.

Restore into an empty database (stops the backend meanwhile):

```bash
cd /apps/talia
docker compose -f compose.prod.yml stop backend
gunzip -c backups/talia-YYYY-MM-DD.sql.gz | docker compose -f compose.prod.yml exec -T db psql -U talia -d talia
docker compose -f compose.prod.yml start backend
```

(To restore over existing data, first recreate the database:
`docker compose -f compose.prod.yml exec db psql -U talia -d postgres -c "DROP DATABASE talia;" -c "CREATE DATABASE talia;"`.)

## 11. Own domain later

1. Point the domain to the server: an `AAAA` record with the server's IPv6 address (with Cloudflare: proxied, SSL mode
   "Full (strict)", and use a Cloudflare origin certificate or keep Caddy's own certificate).
2. In `.env`: `SITE_ADDRESS=mytalia.app`, `PUBLIC_URL=https://mytalia.app`.
3. In `compose.prod.yml` add `"443:443"` to the ports of `web`.
4. `docker compose -f compose.prod.yml up -d`. Caddy requests a Let's Encrypt certificate on the first request.

Devices subscribed to push on the old address must enable notifications again on the new one.
