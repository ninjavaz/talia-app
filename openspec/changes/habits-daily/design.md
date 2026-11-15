# Design

## Context

- `habit (id, user_id, name, sort_order, active, created_at)` and `habit_check (habit_id, day)`. Deleting a habit sets
  `active = false`, so its checks stay; queries filter `active` and `user_id`.
- `HabitService.stats` computes a weekly streak with `user_settings.habit_week_threshold_pct`; `WeekService` embeds
  `HabitStats` into `GET /api/week`.
- The Week screen (`week.ts`) shows the workout panel first, habits below; the app redirects `/` to `/progress`.
- Settings edit habit names with `window.prompt()`; there is no reorder. `@angular/cdk` is already a dependency.
- `IntegrationTest` inserts three habits for "talia"; tests refer to them by id 1-3 and by name.

## Goals / Non-Goals

**Goals:**
- One streak rule, easy to change, unit-testable without the database.
- Past days evaluated against the habits that existed on that day.

**Non-Goals:**
- Reminders at the habit time and the evening "streak at risk" push (notifications change).
- Automatic checking of the steps habit from Apple Health (out of scope).
- Checking past days from the UI (the API keeps accepting past dates, the UI checks today only).

## Decisions

- **Schema (V4)**: `habit.kind VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (kind IN ('NORMAL','STEPS'))`,
  `habit.remind_at TIME NULL`, `habit.created_on DATE NOT NULL` (backfilled from `created_at` in Europe/Warsaw),
  `habit.deactivated_on DATE NULL` (set on delete; for already inactive habits backfilled with `created_on`, i.e. treated
  as never active, so they cannot break a streak). Partial unique index: one active STEPS habit per user. Drop
  `user_settings.habit_week_threshold_pct`. `active` stays as the list filter (no query rewrite needed).
  Alternative: derive activity from `active` only. Rejected: "new habit does not break the past" needs the dates.
- **Streak rule** in `habit/service/StreakRule` (pure Java): `isCompleted(activeHabitIds, doneHabitIds)` and
  `streak(today, days -> completed?)` walking back from today (today only when complete), capped at 3 650 days. The
  service loads all habits of the user (active and deleted) and the checks of the last 3 650 days once, then asks the
  rule per day. Alternative: SQL window functions. Rejected: harder to read and to change ("rule in one place").
- **Steps habit**: stored with `kind = STEPS` and the fixed name `"steps"`; the API returns `kind` and the frontend
  renders "Kroki: 8 000" from `dailyStepsGoal` (already in `GET /api/settings` and `GET /api/week`). Renaming a
  STEPS habit is rejected (400), a second active one is rejected (409 via the unique index, checked in the service
  first for a clean error). Alternative: store the generated name. Rejected: it would go stale when the goal changes.
- **API**: `Habit {id, name, kind, remindAt, sortOrder}`, `HabitDay {id, name, kind, remindAt, done}`,
  `HabitRequest {name, remindAt}` for create/update (`kind` only on create, default NORMAL),
  `PUT /api/habits/order {ids: [...]}` must contain exactly the user's active habit ids (else 400) and rewrites
  `sort_order` 1..n. `HabitStats {doneToday, total, weekPct, streakDays}` (`weekPct` stays for the Week panel, now
  without a threshold). `remindAt` is serialized as `"HH:mm"`.
- **Default habits**: `HabitService.createDefaults(userId, language)` called from `UserAccountService.create`, like
  the default body part. The `IntegrationTest` fixture stops inserting habits: "talia" gets the defaults with ids 1-3
  (1 "Suplementy", 2 "Picie wody", 3 steps). Tests that referred to the old fixture names are adapted to the new
  fixture; the old weekly streak test is replaced by REQ-015 tests.
- **Frontend**: route `''` redirects to `week`; nav order Week, Progress, Settings. Week screen order: header, section
  "Today" with `tl-progress-bar` (x / y), habit list (time as subtitle), streak line, then the workout panel and list.
  Settings: habit list with `cdkDropList`/`cdkDrag` and a drag handle; tapping the edit button opens a `HabitSheet`
  (name + `<input type="time">`); "+" opens the same sheet for a new habit; "Add steps habit" button when none exists.
  The weekly threshold field is removed from the goals form.

## Risks / Trade-offs

- [Streak computation loads up to 10 years of checks] → a few thousand rows at most for one user; fine. Capped.
- [Changing the steps goal renames the habit retroactively] → intended: it is one habit "reach the goal".
- [Old habits deleted before V4 get `deactivated_on = created_on`] → they never count; acceptable (only test data).
- [Login redirect / bookmarks to `/progress`] → `/progress` stays a valid route.

## Migration Plan

V4 runs on startup; rollback = restore the database backup (no production deployment yet).
