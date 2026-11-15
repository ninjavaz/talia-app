# Tasks

## 1. Backend tests from the requirements (written first, expected to fail where behavior changes)

- [x] 1.1 Add REQ-tagged integration tests in `CheckinIntegrationTest`: "REQ-001 …" (weight saved with date, second check-in replaces, invalid values rejected), "REQ-003 several weights in one week give one point dated on the last entry", "REQ-004 weight delta from first to latest week", "REQ-004 no data without weights"; verify they compile and REQ-003/REQ-004 fail on the current code (`mvnw test -Dtest=CheckinIntegrationTest`)
- [x] 1.2 Add "REQ-002 new account starts with a deletable waist" tests in `AuthIntegrationTest` (PL → "Talia", EN → "Waist", rename and delete work) and update `DataIsolationIntegrationTest` (fresh account has one body part); verify they fail on the current code

## 2. Backend implementation

- [x] 2.1 Default body part in `UserAccountService.create` (name by language, sort order 1) and remove the extra insert from the `IntegrationTest` setup; verify REQ-002 tests and the full backend suite pass
- [x] 2.2 Weekly weight points: new repository query (average per week, dated on the last entry), `CheckinService.progress` uses it for `weightPoints` and the weight `Metric`; remove `weightTrend`, `movingAverage` and its unit test; verify REQ-003/REQ-004 tests and the full backend suite pass (`mvnw test`)

## 3. Frontend

- [x] 3.1 Update `api.ts` (`ProgressView` without `weightTrend`), `progress.ts` (weight chart passes `weightPoints` as points and trend) and the label "Waga" / "Weight" in PL and EN; verify `ng build` and `ng test` pass

## 4. E2e (Playwright)

- [x] 4.1 Add `frontend/e2e/measurements.e2e.ts` with "REQ-001 weekly check-in saves weight with date", "REQ-002 new account shows Talia and it can be deleted", "REQ-003 chart shows one point per week", "REQ-004 progress shows difference since first measurement" (data for past weeks seeded through the check-in API); verify `npm run e2e` passes

## 5. Wrap-up

- [x] 5.1 Update CLAUDE.md where it still mentions the moving average / no built-in waist, list the assumptions made, run the full build (backend tests, `ng build`, `ng test`, `npm run e2e`) and verify everything passes
