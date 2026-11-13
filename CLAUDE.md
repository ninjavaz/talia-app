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

## Non-goals
Calorie counting, recipes, multi-user, social features, native iOS app, photo analysis.
Later maybe: pill-break marking, food ideas, native iOS app.

## How to work
- Small steps: backend skeleton -> sync endpoint + tests -> measurements -> frontend shell + PWA -> charts -> habits -> AI summary -> push -> Docker Compose + Caddyfile.
- Keep it simple: no microservices, no premature abstractions, one Spring module.
