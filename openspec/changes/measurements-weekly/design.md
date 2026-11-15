# Design

## Context

- `CheckinService.progress` builds `weightPoints` from daily averages (`WeightEntryRepository.findDailyAverages`) and
  `weightTrend` with `movingAverage(…, 7)`; the weight `Metric` is computed from `weightTrend`.
- `WeightEntryRepository.findWeeklyAverages` already exists (used by the check-in history), keyed by week start.
- Body part points come from `body_measurement`; a check-in deletes the week's manual values before inserting, so
  there is at most one value per part and week already.
- `UserAccountService.create(...)` creates settings only (the default waist was removed in d22dabc / V3).
- The frontend `tl-trend-chart` draws dots for `points` and a line for `trend` (falls back to the points when `trend`
  has fewer than two values); body part charts already pass the same list for both.

## Goals / Non-Goals

**Goals:**
- Weekly weight points with a date the user recognises (the day they weighed themselves).
- One rule for "delta since start" for weight and body parts (`metric(points)`).

**Non-Goals:**
- No schema change, no data migration for existing accounts (their body parts stay as they are).
- No change to the check-in form, the reminder card or the steps value.

## Decisions

- **Weekly weight query**: a new native query groups by `date_trunc('week', measured_at)` and returns
  `max(measured_at)::date` as the point date and `round(avg(value_kg), 1)` as the value, ordered by date.
  The check-in history keeps using week-start keys (`findWeeklyAverages`), so both stay simple.
  Alternative: dating the point on the week start (Monday). Rejected: a Sunday check-in would be shown on Monday.
- **Remove `weightTrend` and `movingAverage`** instead of keeping them unused: the API gets smaller and the frontend
  passes `weightPoints` as both `points` and `trend`, exactly like body parts. The unit test of `movingAverage` goes away
  with the method (the requirement it tested is withdrawn).
- **Default body part name by registration language**: "Talia" (pl) / "Waist" (en), sort order 1, created in
  `UserAccountService.create` through `BodyPartRepository` in the same transaction as the account.
  Alternative: a Flyway seed per user. Rejected: needs to happen at registration time anyway.
- **Test fixture**: `IntegrationTest` stops inserting body part "Talia" itself; account creation provides it with
  id 1. Assertions in existing tests keep their meaning.

## Risks / Trade-offs

- [Clients reading `weightTrend`] → only our own frontend, updated in the same change.
- [Weekly averaging hides day-to-day changes for Health-synced daily weights] → accepted by REQ-003.
- [Existing accounts created between 2026-11-14 and now have no waist] → the user can add it in Settings; no migration.
