# Proposal

## Why

Habits are the daily part of the app and the main risk of dropping it (REQ-052): checking them has to be the first
thing the user sees. Today the habit list has no order editing, no optional time, no default habits, no steps habit, and
the streak is counted in weeks with a percentage threshold, while the requirements ask for a streak of fully completed
days (REQ-010 to REQ-015).

## What Changes

- REQ-010: habits can be added, edited (name, time), deleted and **reordered by dragging** in Settings. A new account
  starts with three habits: "Suplementy", "Picie wody" (EN "Supplements", "Drinking water") and the steps habit.
- REQ-011: every habit can have an optional time of day (e.g. each supplement as its own habit with its own time).
  The time is shown on the daily list; reminders at that time come with the notifications change.
- REQ-012: one-tap daily check stays as it is (REQ-tagged tests added).
- REQ-013: a special **steps habit** ("Kroki: 8 000"): checked by hand when the daily steps goal is reached, the goal
  comes from Settings, no integration. At most one per user; its name follows the goal and cannot be edited.
- REQ-014: a daily progress bar "x / y today" above the habit list.
- REQ-015: **BREAKING** the streak is counted in **days**: a day counts when every habit that was active on that day is
  checked; streak = consecutive counted days. Today counts once complete and never breaks the streak while it is still
  incomplete. A habit added later does not affect earlier days. The rule lives in one class. The weekly threshold
  setting (`habitWeekThresholdPct`) is removed from the API, the UI and the database.
- REQ-052: the **Week screen becomes the home screen**, reordered: daily progress, habits and streak first, workouts
  below. Checking a habit = 1 tap; adding a workout = 2 taps ("+" and "Save" with defaults). Bottom nav order:
  Week, Progress, Settings.

Contradictions resolved (agreed with the user on 2026-11-15):
- CLAUDE.md "streak counted in WEEKS (week counts if >= X% done)": replaced by a streak in days.
- CLAUDE.md "a new account has no habits" (implicit): new accounts get three default habits.
- Home screen was Progress; now Week.

## Capabilities

### New Capabilities
- `habits`: daily habit list (configuration, order, optional time, steps habit), daily check, progress and streak.
- `home-screen`: the start screen and the tap budget for daily entries.

### Modified Capabilities
<!-- none -->

## Impact

- Database: migration V4 (`habit.kind`, `habit.remind_at`, `habit.created_on`, `habit.deactivated_on`, one active steps
  habit per user; drop `user_settings.habit_week_threshold_pct`).
- Backend: `habit` package (entity, repositories, `HabitService`, new streak rule, controller: reorder endpoint,
  request/response models), `UserAccountService` (default habits), `settings` (threshold removed), `WeekService`.
- API: `Habit`/`HabitDay` get `kind` and `remindAt`; `HabitStats` becomes `{doneToday, total, weekPct, streakDays}`;
  new `PUT /api/habits/order`; `AppSettings` without `habitWeekThresholdPct`.
- Tests: `HabitIntegrationTest` (old weekly streak test replaced), `IntegrationTest` fixture (default habits instead of
  three inserted ones), `ActivityIntegrationTest`, `DataIsolationIntegrationTest`, `AuthIntegrationTest`.
- Frontend: routes and bottom nav, `week.ts`, `settings.ts` (habit list with drag & drop, habit sheet), `api.ts`,
  translations PL/EN. E2e: `frontend/e2e/habits.e2e.ts`, smoke test (home screen).
