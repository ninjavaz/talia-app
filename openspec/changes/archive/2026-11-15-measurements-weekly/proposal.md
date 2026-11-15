# Proposal

## Why

Measurements are entered once a week, but the weight chart still draws daily points with a 7-day moving average,
and the weight delta is computed from that average instead of the real measurements. Since 2026-11-14 a new account
also starts without any body part, while the requirements expect waist tracking from day one. This change aligns the
measurements with REQ-001 to REQ-004.

## What Changes

- REQ-001: no behavior change. The weekly check-in already stores a manual weight in kg with a date (one check-in per
  week, saving again replaces it). The change adds REQ-tagged tests that pin this behavior.
- REQ-002: a new account gets one body part again, "Talia" (PL) or "Waist" (EN) depending on the registration
  language. It is an ordinary body part: it can be renamed and deleted. Existing accounts are not changed.
- REQ-003: the weight chart shows one point per week (the average of that week's entries, dated on the week's last
  entry). **BREAKING** (API): `weightTrend` is removed from `GET /api/progress`; `weightPoints` becomes weekly.
  Body part charts already have at most one point per week (a check-in replaces the week's values).
- REQ-004: the weight delta is computed from the weekly points (first week vs. latest week) instead of the moving
  average; the "Weight (trend)" label becomes "Weight". Body part deltas already work this way.

Contradictions resolved (agreed with the user on 2026-11-15):
- CLAUDE.md "chart with raw points + 7-day moving average": replaced by one point per week.
- CLAUDE.md "No built-in waist (2026-11-14)": new accounts get a deletable "Talia" body part again.

## Capabilities

### New Capabilities
- `measurements`: weekly check-in of weight and body measurements, the progress chart and the deltas since the start.

### Modified Capabilities
<!-- none: there are no main specs yet -->

## Impact

- Backend: `UserAccountService.create` (default body part), `CheckinService.progress` (weekly weight points, metric),
  `WeightEntryRepository` (weekly averages dated on the last entry), `ProgressView` (without `weightTrend`).
- Tests: `CheckinIntegrationTest`, `DataIsolationIntegrationTest` (fresh account has one body part),
  `IntegrationTest` setup (the default body part now comes from account creation instead of an extra insert).
- Frontend: `api.ts` (`ProgressView`), `progress.ts` (chart uses weekly points, label), translations PL/EN.
- E2e: new `frontend/e2e/measurements.e2e.ts`.
- No database migration (no schema change).
