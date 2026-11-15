# Design

## Context

- `WorkoutService.between` maps `workout` rows to `Workout(id, startedAt, durationMin, type, category, kcal)`;
  `kcal` is only filled by Apple Health. `WeekService.week` builds `WeekView` from it.
- Weights live in `weight_entry` (manual check-in and Health), owned by the `measurement` package.

## Goals / Non-Goals

**Goals:**
- One place for the formula and MET table, unit-testable.
- No stored estimates: a weight entered later or a corrected weight changes the numbers.

**Non-Goals:**
- Heart-rate based or intensity-based estimates; editing a workout's kcal by hand.

## Decisions

- **`CalorieEstimate`** (pure, `activity/service`): `MET = {GYM 5.0, PILATES 3.0, WALK 3.5, OTHER 4.0}` (Compendium
  of Physical Activities: resistance training ~3.5-6, pilates ~3.0, walking ~3.5 at a moderate pace, a middle value
  for other); `kcal = round(MET x kg x minutes / 60)`.
- **Weight lookup** in a small `measurement/service/WeightService.weightOn(userId, day)` with two derived queries
  (`findFirstByUserIdAndMeasuredAtLessThanOrderByMeasuredAtDesc`, `findFirstByUserIdOrderByMeasuredAtAsc`), so the
  `activity` package does not use measurement repositories directly. One lookup per workout (a few per week).
- **Model**: `Workout.kcal` becomes `Integer` (rounded; Health's decimal value is rounded too) plus
  `kcalEstimated`; `WeekView.kcalTotal` = sum of the week's non-null kcal, null when none.
- **Frontend**: subtitle "wt., 10.11 · 60 min · ~320 kcal" ("~" only for estimates); the workouts panel gets one
  line "ok. 495 kcal spalone w tym tygodniu" under the ring when `kcalTotal` is present.

## Risks / Trade-offs

- [MET values are rough] → shown with "~" / "ok." and documented here; easy to tune in one map.
- [Users may read it as calorie counting] → burned calories only, no food input anywhere.
