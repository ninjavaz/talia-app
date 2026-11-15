# Talia (shown to users as "MyTalia") – project brief

Personal progress-tracking PWA for ONE user (iPhone). Dead-simple tracking of waist + weight trend,
activity and daily habits. **NO calorie counting.** UI language: **Polish**. Mobile-first, installed as PWA on iOS home screen.

## Stack
- Frontend: Angular (latest, standalone components, signals), @angular/pwa, minimal UI (no heavy UI lib), simple SVG charts
- Backend: Spring Boot 3 (Java 21), Spring Data JPA, Flyway, PostgreSQL
- Deploy: Docker Compose on VPS + Caddy (auto HTTPS)
- Auth: single user, username/password -> session cookie. Separate static API token (stored as SHA-256 hash) only for the health-sync endpoint.

## MVP scope (build ONLY this)
1. Measurements – weight (auto from Apple Health via sync + manual fallback), waist cm (manual, weekly), chart with one point per week (weekly average, no moving average - REQ-003), delta since start (kg, cm).
2. Activity (from Apple Health via sync) – daily steps, workouts (type, duration, date), weekly workout goal with progress bar.
3. Habits – configurable daily habits (order, optional time, steps habit), one-tap checkbox per day; streak counted in
   DAYS (a day counts when all habits active that day are checked; rule in `StreakRule`, REQ-015).
4. Weekly AI summary – Sunday job: aggregate last 7 days -> LLM (provider behind interface, key in env) -> store short Polish summary (≤ ~5 sentences, supportive but honest).
5. Web Push – weekly waist reminder; summary ready; alert if no health sync for 48h.

## Health sync endpoint
`POST /api/health-sync`, `Authorization: Bearer <token>`
```json
{ "steps":[{"date":"2026-11-08","value":7432}],
  "weight":[{"ts":"2026-11-08T07:12:00","value":68.4}],
  "workouts":[{"start":"...","end":"...","type":"...","kcal":0}] }
```
- Idempotent upsert: steps by date, weight by ts, workouts by start+type
- Client (iOS Shortcut) sends last 3 days every time; duplicates must be harmless
- Returns counts of inserted/updated
- Integration tests with Testcontainers

## Decisions (agreed 2026-11-13)
- **Spring Boot 4.1 + Java 25** (overrides "Boot 3 / Java 21" above). Maven wrapper in `backend/`.
- **Multi-user (2026-11-13, overrides "single user" above)**: open registration in the app, every table has `user_id`
  (or belongs to a user's body_part/habit) and **every query filters by the logged-in user** (`@AuthenticationPrincipal TaliaUser`).
  Foreign ids behave like missing ones (404/400). `DataIsolationIntegrationTest` guards this - extend it for new endpoints.
- Security: bcrypt cost 12; password min 10 chars, max 72 bytes, small deny-list; usernames lowercase `[a-z0-9._-]{3,30}`.
  Registration bot check = official ALTCHA (Java `org.altcha:altcha` v2 PBKDF2, deterministic mode, in `BotCheckService`;
  npm widget `altcha` in `AltchaField`), each solution used once (`altcha_used`) + honeypot `website` + per-IP limits.
  Rate limits with **Bucket4j** stored in Postgres (`rate_limit_bucket`, ids = SHA-256 of key) in `RateLimitService`:
  failed logins per IP and per username (429 + Retry-After), registrations, challenges, e-mails per IP and per address.
- **E-mail (2026-11-13)**: registration requires an e-mail; login only after confirming it (403 `EMAIL_NOT_VERIFIED`,
  a new link is sent on that login attempt). Password reset by e-mail (`/forgot-password`, `/reset-password`, ends all
  sessions). Links `/verify#t=` and `/reset#t=` (token in the fragment, only SHA-256 stored in `email_token`).
  Answers never reveal whether an address has an account. Unconfirmed accounts are deleted after 48 h (`AccountCleanupService`).
  Sending: plain SMTP (`spring.mail.*`, env `MAIL_*`), provider-agnostic, `@Async` `MailService`; locally Mailpit
  (compose.dev.yml, UI http://localhost:8025). Tests mock `MailService`. Session: spring-session-jdbc, 365 days sliding,
  cookie defined in `SessionCookieConfig` (HttpOnly, Secure, SameSite=Strict = CSRF defense); `/api/auth/me`
  re-issues it on every app start. Per-user Apple Health sync token: random 256 bit, only SHA-256 stored, shown once.
- Migrations were squashed into a single V1 before the first deployment; new changes go into new migrations (V2...).
- Weekly workout goal = one number; **all workouts of the week count** (also walks). Workouts get a category
  (GYM/PILATES/WALK/OTHER) derived from the Health type name (PL+EN keywords) or chosen when added manually.
- **Manual-first (2026-11-13)**: once a week (Sunday) a *check-in* with waist, weight and the week's **average daily steps**
  (table `weekly_steps`, wins over synced daily steps). One check-in per week, saving again replaces it.
  In-app reminder card on Progress when due (Sunday, or >7 days since the last waist). Habits are a daily checklist on the
  Week screen (the home screen since REQ-052), workouts are added manually there. Health sync stays as an optional automatic source.
- **Custom body measurements**: `body_part` and `body_measurement`. A new account starts with one ordinary body part
  "Talia"/"Waist" (by registration language, REQ-002, 2026-11-15); the user adds more in Settings (all
  renamable/deletable). The headline tile on Progress shows the first body part of the list. The check-in sends `measurements: [{partId, valueCm}]` and
  *replaces* the week's manual data.
- UX: every save shows a green `TlToast` (`<tl-toaster>` in the app root); checking a habit fires `diamondBurst()` 💎
  (bigger when all habits are done); clickable things have `cursor: pointer` and a subtle hover scale.
- Pages read `httpResource` data through `safeValue()` (resource.value() throws in error state).
  Form fields bound with ngModel are signals (zoneless: plain fields set asynchronously don't re-render).
- Goals and habits are **configurable in the UI** (`user_settings` row per user + `habit` table). New accounts get the
  habits "Suplementy", "Picie wody" and the steps habit (`habit.kind = STEPS`, shown as "Kroki: <goal>", one per user).
  A habit counts for the streak from `created_on` until `deactivated_on` (delete = deactivate).
- **AI summary postponed to v2** and hidden from the UI for now. Provider will be Anthropic behind `LlmClient`. No `weekly_summary` table yet.
- **Push (2026-11-15, REQ-030/031/033/034)**: package `push`. Web Push implemented with JDK crypto only (`WebPushCrypto`
  = RFC 8291, verified with the RFC test vector; `Vapid` = RFC 8292). VAPID keys from env `VAPID_PUBLIC_KEY` /
  `VAPID_PRIVATE_KEY` / `VAPID_SUBJECT` (`scripts\vapid-keys.cmd`; without them a random pair per start, dev only).
  SSRF guard: only `talia.push.allowed-host-suffixes` over HTTPS (`PushEndpoints`). `PushReminderService.sendDue()`
  runs every minute (`PushScheduler`, off in tests via `talia.push.reminders-enabled=false`): habit at its time,
  weekly check-in (default Sun 10:00), streak at risk (default 20:00); due = within the last 30 min, sent once via
  `push_sent` claim. Tests use the shared `pushSender` mock from `IntegrationTest`. No 48h-sync alert; "summary ready"
  (REQ-032) comes with the AI summary in v2.
- **Light/dark theme** (system default + manual toggle, stored per device) and **i18n PL + EN** (PL default; language in `app_settings`, also used for push texts).
- **Frontend**: Angular 22 (zoneless, standalone, signals), Tailwind 4 via postcss, **Spartan UI** like the cook-less project
  (brain from npm, helm generated into `frontend/projects/ui/helm`, alias `@ui/*`, style `nova`). **No Nx**, plain Angular CLI.
- **Design system = library `frontend/projects/ui` (`@talia/ui`, prefix `tl-`)**, path-mapped to source. Visual style inspired by
  a calm wellness app: cream background, white rounded cards, charcoal header bands, sage green progress rings, floating dark
  pill bottom nav, Montserrat (self-hosted via @fontsource). Tokens in `src/styles.css` (`--panel`, `--chip`, `--track`,
  `--progress`, `--nav`...). Build screens from `tl-*` components; living style guide at route `/ui`.
  i18n: own signal-based dictionary (`core/i18n/translations.ts`, `| t` pipe), not `@angular/localize`.
- Local dev: `scripts\dev.cmd` (Windows .cmd with CRLF; call `.\mvnw.cmd` with an explicit path).
- Times stored as local `timestamp` in `Europe/Warsaw` (`talia.timezone`); sync accepts ISO with or without offset.
- **Requirements REQ-001... (2026-11-15)**, implemented as OpenSpec changes (one per group: measurements, habits,
  activity, notifications). Agreed with the user (overriding older bullets above where they differ):
  new accounts get a deletable "Talia" body part again; charts show one point per week (no 7-day moving average);
  delta from the first real measurement; habit streak counted in **days** (a day counts when all habits active on that
  day are checked; a habit added later does not break the earlier streak), rule kept in one place; steps are a special
  habit type (goal from settings) and the steps field of the check-in stays; workout duration stays required;
  home screen = today's habits + quick workout add (every daily entry <= 2 taps); Web Push in the PWA now, incl. an
  evening push when the day is not complete yet (streak at risk); estimated burned kcal
  (REQ-022: MET gym 5 / pilates 3 / walk 3.5 / other 4 x weight on that day x hours, computed on read in
  `CalorieEstimate`; Health kcal > 0 wins) on the workout list and as a weekly sum (burned, not food calories); WALK stays a normal
  workout type that counts towards the weekly goal (no extra setting);
  **AI summary (REQ-040/041) and "summary ready" push (REQ-032) stay in v2**.

## Non-goals
Calorie counting, recipes, multi-user, social features, native iOS app, photo analysis.
Later maybe: pill-break marking, food ideas, native iOS app.

## How to work
- **Language (2026-11-15)**: everything written into the repo or GitHub is in **English**: documentation, README,
  OpenSpec artifacts, code comments, commit messages, PR titles and descriptions. The UI stays PL + EN (i18n);
  conversation with the user is in Polish.
- Small steps: backend skeleton -> sync endpoint + tests -> measurements -> frontend shell + PWA -> charts -> habits -> AI summary -> push -> Docker Compose + Caddyfile.
- Keep it simple: no microservices, no premature abstractions, one Spring module.
- **Deployment (2026-11-15)**: Mikrus VPS (1 GB RAM) at `https://<server>.mikrus.cloud` (TLS by the Mikrus proxy,
  Caddy serves plain `:80`; own domain later via `SITE_ADDRESS`). `deploy/` = `compose.prod.yml` (db, backend, web =
  Caddy + Angular build, backup), `Caddyfile` (CSP, client IP only from `TRUSTED_PROXIES`), `backup.sh`,
  `.env.example`. Images built by `.github/workflows/ci.yml` on `main` -> `ghcr.io/ninjavaz/talia-{backend,web}`
  (`latest` + SHA). The user runs the server commands himself from `docs/deploy.md`. No inline scripts in
  `index.html` (CSP `script-src 'self'`).
- **Git flow (2026-11-15)**: `main` = released, `develop` = integration. Every requirement group / OpenSpec change on
  its own branch from `develop` (e.g. `feature/measurements-weekly`), pushed and opened as a PR into `develop` with
  `gh pr create --base develop`; the user decides when to merge (merging happens from here with `gh pr merge`).
  `develop` -> `main` via PR when a set of groups is done. gh CLI: `C:\Program Files\GitHub CLI\gh.exe`.
- **OpenSpec** (`openspec/`, rules in `openspec/config.yaml`, commands `/opsx:propose`, `/opsx:apply`, `/opsx:archive`):
  one change per requirement group with REQ-IDs; order migration -> backend + Testcontainers tests -> frontend ->
  Playwright e2e; tests first, REQ-ID in every test name, tasks up to ~200 lines of diff, full build after each task.
- **E2e (Playwright)**: `cd frontend && npm run e2e` (Docker needed). Starts its own backend (`E2eTaliaApplication`
  in the test sources: Testcontainers Postgres, port 8081, profile `e2e`) and `ng serve` on 4300. Every test starts
  with `POST /api/e2e/reset` (test-only functional endpoint in `E2eSupport`, never in the jar): clean DB + account
  `e2e`. Tests in `frontend/e2e/*.e2e.ts`, fixtures `test`/`loggedIn` from `e2e/fixtures.ts`, phone viewport.
- **Backend layout (2026-11-14)**: every feature package (`auth`, `activity`, `habit`, `measurement`, `settings`, `sync`)
  is split into `controller/` (thin REST controllers: HTTP only, no logic), `service/` (`*Service`, business logic and
  transactions, no SQL), `repository/` (Spring Data JPA interfaces; derived queries / JPQL first, native `@Query` only
  for Postgres-specific SQL such as `ON CONFLICT ... RETURNING (xmax = 0)` upserts and `date_trunc` aggregations),
  `entity/` (`*Entity` JPA classes mapping one table each, `userId` as a plain column instead of relations) and
  `model/` (API records/DTOs, enums, exceptions - one type per file). Spring/security wiring in `auth/config/`.
  Flyway owns the schema, Hibernate only validates it (`ddl-auto: validate`).