# Tasks

## 1. Database

- [x] 1.1 Migration `V4__habits_daily.sql` (kind, remind_at, created_on, deactivated_on with backfill, unique active steps habit, drop habit_week_threshold_pct) and the matching entity/settings fields; verify the backend starts against it (Hibernate validate) and the existing suite still passes after removing the threshold from `AppSettings` and its tests

## 2. Backend tests from the requirements (written first)

- [x] 2.1 `StreakRule` unit tests "REQ-015 ..." (consecutive days, incomplete today does not break, today completes, missed day breaks, new habit does not change past days, day without habits does not count); verify they fail (rule not implemented yet)
- [x] 2.2 Integration tests in `HabitIntegrationTest` "REQ-010 ..." (defaults PL/EN, add/edit/delete, reorder, invalid reorder), "REQ-011 ..." (time set, cleared, invalid), "REQ-012 ..." (check/uncheck idempotent, future day), "REQ-013 ..." (one steps habit, not renamable, checkable), "REQ-014 ..." (doneToday/total), "REQ-015 ..." (streak over the API incl. habit added today and deleted habit); switch the `IntegrationTest` fixture to the default habits and replace the old weekly streak test; extend `DataIsolationIntegrationTest` (reorder/update with foreign ids); verify the new tests fail for the right reason

## 3. Backend implementation

- [x] 3.1 `StreakRule` + `HabitService.stats` with `streakDays`; verify the REQ-015 tests pass
- [x] 3.2 Habit kind/time/created_on/deactivated_on in service and models, default habits on account creation, reorder endpoint, steps habit rules; verify all backend tests pass (`mvnw test`)

## 4. Frontend

- [x] 4.1 `api.ts` types, home route and nav order, Week screen reordered (daily progress bar, habits with time, streak in days, steps habit label); verify `ng build` and `ng test` pass
- [x] 4.2 Settings: habit sheet (name + time), drag & drop order, add steps habit, threshold field removed, translations PL/EN; verify `ng build` and `ng test` pass

## 5. E2e (Playwright)

- [ ] 5.1 `frontend/e2e/habits.e2e.ts`: "REQ-010 default habits and reorder by dragging", "REQ-011 habit with a time", "REQ-012 check and uncheck with one tap", "REQ-013 steps habit shows the goal", "REQ-014 daily progress", "REQ-015 streak in days" (past checks seeded via API), and `home.e2e.ts` "REQ-052 app opens on the Week screen / habit in one tap / workout in two taps"; update the smoke test for the new start page; verify `npm run e2e` passes

## 6. Wrap-up

- [ ] 6.1 Update CLAUDE.md (streak in days, default habits, home screen), list assumptions in design.md, full build (backend tests, `ng build`, `ng test`, `npm run e2e`), archive the change in the same PR
