# Talia

A simple PWA for tracking body measurements, weight trend, activity and habits. No calorie counting.

- `backend/`: Spring Boot 4.1, Java 25, PostgreSQL, Flyway
- `frontend/`: Angular 22, Tailwind 4, Spartan UI (helm in `projects/ui/helm`), PWA
- `scripts/`: scripts for running locally (Windows)
- `openspec/`: requirements as OpenSpec changes and specs
- `deploy/`: production Docker Compose stack, see [docs/deploy.md](docs/deploy.md)

Details and design decisions: [CLAUDE.md](CLAUDE.md).

## Running locally

Requirements: JDK 25, Node 24+, Docker Desktop (running).

```bat
scripts\dev.cmd
```

The script opens two windows:

| | Address | Script |
|---|---|---|
| Backend | http://localhost:8080 | `scripts\dev-backend.cmd` (starts the database first), or `cd backend` + `.\mvnw.cmd` |
| Frontend | http://localhost:4200 | `scripts\dev-frontend.cmd` (`npm start`, `/api` proxied to :8080) |

Create an account in the app ("Create account" tab). Locally, e-mails with the confirmation link (and password reset) never
leave your computer: **Mailpit** catches them at http://localhost:8025. The personal Apple Health sync token is
generated in Settings.

`.\mvnw.cmd` without arguments starts the backend with the `dev` profile. Local settings live in
[backend/config/application-dev.yml](backend/config/application-dev.yml), which is not packaged into the jar. The
database must be running then (`scripts\db-start.cmd`).

After changes to `tsconfig.json` (e.g. new Spartan components) restart `npm start`; the watch server does not reload
path aliases.

### Sending e-mails (production)

The backend sends e-mails over plain SMTP, so the provider is switched by configuration only (environment variables):

| Variable | Example |
|---|---|
| `MAIL_HOST`, `MAIL_PORT` | `smtp-relay.brevo.com`, `587` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP login / key from the provider |
| `MAIL_FROM` | `Talia <no-reply@your-domain.com>` |
| `PUBLIC_URL` | `https://talia.your-domain.com` (links in e-mails) |
| `ALTCHA_HMAC_SECRET` | a long random string (optional; without it a random one is generated on every start) |

The sender domain needs SPF and DKIM (the provider gives the DNS records), otherwise e-mails end up in spam.

### Push notifications (production)

Reminders are sent with Web Push. The server needs a fixed VAPID key pair, otherwise every restart uses a new random
pair and devices have to subscribe again:

| Variable | Example |
|---|---|
| `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` | printed by `scriptsapid-keys.cmd` (keep the private key secret) |
| `VAPID_SUBJECT` | `mailto:admin@your-domain.com` (contact for the push services) |

Push only works over HTTPS in a production build (the service worker is off in `ng serve`); on iPhone only in the
app added to the home screen (iOS 16.4+). The server sends only to the known push services (Google, Mozilla, Apple,
Microsoft), see `talia.push.allowed-host-suffixes`.

### Database

The local Postgres 18 runs in Docker ([compose.dev.yml](compose.dev.yml)) on **localhost:5433**, database `talia`,
user `talia` / `talia` (e.g. for DBeaver). Data lives in a Docker volume, so it survives restarts.

| Script | What it does |
|---|---|
| `scripts\db-start.cmd` | starts the database and Mailpit, waits until they are ready |
| `scripts\db-reset.cmd` | deletes the database including its data (asks for confirmation) |

Flyway creates the schema when the backend starts. Full reset: `db-reset`, then `dev-backend`.

Alternative without a local database: [TestTaliaApplication](backend/src/test/java/app/talia/TestTaliaApplication.java)
(run from the IDE) starts a temporary Postgres through Testcontainers.

Sync test:

```bash
curl -H "Authorization: Bearer <token from Settings>" -H "Content-Type: application/json" \
  -d '{"steps":[{"date":"2026-11-13","value":5321}]}' http://localhost:8080/api/health-sync
```

## Tests

```bat
cd backend && .\mvnw.cmd verify      :: integration tests (needs Docker)
cd frontend && npm test -- --watch=false
cd frontend && npm run e2e           :: Playwright e2e (needs Docker; own backend on :8081, frontend on :4300)
```
