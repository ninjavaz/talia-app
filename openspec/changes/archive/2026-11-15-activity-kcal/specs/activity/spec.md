# Spec Delta

## Purpose

Lets the user log workouts by hand, follow a weekly workout goal and see roughly how many calories the workouts
burned.

## ADDED Requirements

### Requirement: REQ-020 Manual workout
The user SHALL be able to add a workout with a date (not in the future), a type (gym, pilates, walk, other) and a
duration of 1-600 minutes; the duration is required. The workout SHALL appear in the list of its week and SHALL be
deletable. Invalid input SHALL be rejected without storing anything.

#### Scenario: Workout added
- **WHEN** the user adds pilates for 50 minutes today
- **THEN** the week's list shows "Pilates" with 50 min and the week's workout count grows by one

#### Scenario: Invalid workout
- **WHEN** a workout with a future date, an unknown type, a duration of 0 or no duration is sent
- **THEN** it is rejected and nothing is stored

#### Scenario: Workout deleted
- **WHEN** the user deletes a workout
- **THEN** it disappears from the list and no longer counts

### Requirement: REQ-021 Weekly workout goal with progress
The user SHALL be able to set a weekly workout goal (0-21) in Settings. The Week screen SHALL show the number of the
week's workouts against the goal as a progress ring. Every workout type counts, including walks.

#### Scenario: Progress towards the goal
- **WHEN** the goal is 3 and the user has two workouts this week
- **THEN** the Week screen shows 2 of 3

#### Scenario: Goal changed
- **WHEN** the user sets the goal to 4
- **THEN** the Week screen shows the progress against 4

#### Scenario: Invalid goal
- **WHEN** a goal above 21 is saved
- **THEN** it is rejected

### Requirement: REQ-022 Estimated burned calories
Each workout SHALL show an estimate of burned kcal = MET x weight (kg) x duration (h), rounded to whole kcal, with MET
gym 5.0, pilates 3.0, walk 3.5, other 4.0. The weight SHALL be the latest weight entry on or before the workout's day,
otherwise the earliest entry after it; without any weight there SHALL be no estimate. A kcal value above 0 synced
from Apple Health SHALL be shown instead of the estimate and marked as not estimated. The Week screen SHALL show the
sum of the week's workout kcal. The estimate SHALL be computed when read, so a weight entered later is used.

#### Scenario: Estimate from the latest weight
- **WHEN** the latest weight before a 60-minute gym workout is 64 kg
- **THEN** the workout shows about 320 kcal

#### Scenario: Weight entered after the workout
- **WHEN** the only weight is 70 kg, entered two days after a 50-minute pilates workout
- **THEN** the workout shows about 175 kcal

#### Scenario: No weight
- **WHEN** the user has no weight entry
- **THEN** the workout shows no kcal and the weekly sum is empty

#### Scenario: Synced kcal wins
- **WHEN** a workout synced from Apple Health has 250 kcal
- **THEN** it shows 250 kcal, not an estimate

#### Scenario: Weekly sum
- **WHEN** this week has workouts with 320 and 175 kcal
- **THEN** the Week screen shows about 495 kcal burned this week
