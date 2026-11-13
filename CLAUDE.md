# Talia – project brief

Personal progress-tracking PWA for ONE user (iPhone). Dead-simple tracking of waist + weight trend,
activity and daily habits. **NO calorie counting.** UI language: **Polish**. Mobile-first, installed as PWA on iOS home screen.

## Stack
- Frontend: Angular (latest, standalone components, signals), @angular/pwa, minimal UI (no heavy UI lib), simple SVG charts
- Backend: Spring Boot 3 (Java 21), Spring Data JPA, Flyway, PostgreSQL
- Deploy: Docker Compose on VPS + Caddy (auto HTTPS)
- Auth: single user, username/password -> session cookie. Separate static API token (stored as SHA-256 hash) only for the health-sync endpoint.

## MVP scope (build ONLY this)
1. Measurements – weight (auto from Apple Health via sync + manual fallback), waist cm (manual, weekly), chart with raw points + 7-day moving average, delta since start (kg, cm).
2. Activity (from Apple Health via sync) – daily steps, workouts (type, duration, date), weekly workout goal with progress bar.
3. Habits – configurable 3–4 daily habits, one-tap checkbox per day; streak counted in WEEKS (week counts if ≥ X% done).
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
- Single user; credentials from env: `TALIA_USERNAME`, `TALIA_PASSWORD_HASH` (DelegatingPasswordEncoder, e.g. `{bcrypt}$2a$...`),
  `TALIA_SYNC_TOKEN_SHA256` (hex). No user/token tables. Session: spring-session-jdbc, 90 days, cookie SameSite=Strict (CSRF defense, CSRF tokens disabled).
- Weekly workout goal = one number; **all workouts of the week count** (also walks). Workouts get a category
  (GYM/PILATES/WALK/OTHER) derived from the Health type name (PL+EN keywords) or chosen when added manually.
- **Manual-first (2026-11-13)**: once a week (Sunday) a *check-in* with waist, weight and the week's **average daily steps**
  (table `weekly_steps`, wins over synced daily steps). One check-in per week, saving again replaces it.
  In-app reminder card on Progress when due (Sunday, or >7 days since the last waist). Habits are a daily checklist on the
  Week screen, workouts are added manually there. Health sync stays as an optional automatic source.
- **Custom body measurements** (V3): `body_part` (+ built-in `code='WAIST'`, not deletable) and `body_measurement`.
  The check-in sends `measurements: [{partId, valueCm}]` and *replaces* the week's manual data. Waist stays the headline metric.
- UX: every save shows a green `TlToast` (`<tl-toaster>` in the app root); checking a habit fires `diamondBurst()` 💎
  (bigger when all habits are done); clickable things have `cursor: pointer` and a subtle hover scale.
- Pages read `httpResource` data through `safeValue()` (resource.value() throws in error state).
  Form fields bound with ngModel are signals (zoneless: plain fields set asynchronously don't re-render).
- Goals, thresholds and habits are **configurable in the UI** (`app_settings` single row + `habit` table).
- **AI summary postponed to v2** and hidden from the UI for now. Provider will be Anthropic behind `LlmClient`. No `weekly_summary` table yet.
- Push in MVP: weekly waist reminder + no-sync-for-48h alert ("summary ready" comes with v2).
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

## Non-goals
Calorie counting, recipes, multi-user, social features, native iOS app, photo analysis.
Later maybe: pill-break marking, food ideas, native iOS app.

## How to work
- Small steps: backend skeleton -> sync endpoint + tests -> measurements -> frontend shell + PWA -> charts -> habits -> AI summary -> push -> Docker Compose + Caddyfile.
- Keep it simple: no microservices, no premature abstractions, one Spring module.
