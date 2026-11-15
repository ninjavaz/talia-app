# Proposal

## Why

Manual workouts and the weekly workout goal already exist (REQ-020, REQ-021) but are not pinned by REQ-tagged tests.
The user also asked (2026-11-15) for an estimate of the calories a workout burned, shown on the workout list and as a
weekly sum - a motivating number that needs no extra input.

## What Changes

- REQ-020 (no behavior change): manual workout with date (not in the future), type (Siłownia, Pilates, Spacer, Inne)
  and a required duration (1-600 min). WALK stays a normal type that counts towards the goal. REQ-tagged tests.
- REQ-021 (no behavior change): configurable weekly workout goal (0-21) with the progress ring on the Week screen.
  REQ-tagged tests.
- **REQ-022 (new ID for the agreed extension)**: estimated burned kcal per workout = MET of the type x body weight x
  hours. Weight = latest weight entry on or before the workout day, otherwise the first one after it; without any
  weight there is no estimate. A kcal value synced from Apple Health (> 0) is shown instead of the estimate. The Week
  screen shows the week's sum. Burned calories only - no food calorie counting.

## Capabilities

### New Capabilities
- `activity`: manual workouts, the weekly goal and the burned-calorie estimate.

### Modified Capabilities
<!-- none -->

## Impact

- Backend: `activity` (`CalorieEstimate` rule, `WorkoutService`, `Workout` model with `kcal` as a rounded integer and
  `kcalEstimated`, `WeekView.kcalTotal`), `measurement` (`WeightService.weightOn`, two derived queries).
- API: `Workout.kcal` becomes an integer, new `Workout.kcalEstimated`, new `WeekView.kcalTotal`.
- Frontend: workout subtitle "60 min · ~320 kcal", weekly sum in the workouts panel, translations PL/EN.
- No migration.
