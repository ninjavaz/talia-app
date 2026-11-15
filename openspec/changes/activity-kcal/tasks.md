# Tasks

## 1. Backend tests from the requirements (written first)

- [x] 1.1 `CalorieEstimateTest` "REQ-022 ..." (MET per type, rounding, no weight) and REQ-tagged integration tests in `ActivityIntegrationTest`: "REQ-020 ..." (add, invalid incl. missing duration, delete, walk counts), "REQ-021 ..." (progress, goal change, invalid goal), "REQ-022 ..." (latest weight, weight after the workout, no weight, synced kcal wins, weekly sum); verify the REQ-022 tests fail on the current code

## 2. Backend implementation

- [x] 2.1 `CalorieEstimate`, `WeightService.weightOn`, `Workout.kcal`/`kcalEstimated`, `WeekView.kcalTotal`; verify all backend tests pass (`mvnw test`)

## 3. Frontend

- [ ] 3.1 Types in `api.ts`, kcal in the workout subtitle, weekly sum in the workouts panel, translations PL/EN; verify `ng build` and `ng test` pass

## 4. E2e (Playwright)

- [ ] 4.1 `frontend/e2e/activity.e2e.ts`: "REQ-020 workout added with type, date and duration", "REQ-021 weekly goal and progress", "REQ-022 estimated kcal on the list and as weekly sum"; verify `npm run e2e` passes

## 5. Wrap-up

- [ ] 5.1 Update CLAUDE.md, list assumptions in design.md, full build (backend tests, `ng build`, `ng test`, `npm run e2e`), archive the change in the same PR
