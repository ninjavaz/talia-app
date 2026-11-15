# Spec Delta

## Purpose

Lets the user record weight and body measurements once a week and see how they change over time: a weekly chart
and the difference since the first measurement.

## ADDED Requirements

### Requirement: REQ-001 Manual weight entry with a date
The user SHALL be able to enter a weight in kg (20-400, one decimal) with a date that is not in the future, as part of
the weekly check-in. The check-in SHALL hold at most one entry per week: saving again for a date in the same week
SHALL replace the earlier manual values of that week. Weights synced from Apple Health SHALL stay untouched.

#### Scenario: Weight saved with its date
- **WHEN** the user saves a check-in with weight 68.4 kg dated today
- **THEN** the check-in history shows 68.4 kg for the current week

#### Scenario: Second check-in in a week replaces the first
- **WHEN** the user saves 68.4 kg on Monday and 68.0 kg on Sunday of the same week
- **THEN** the week shows only 68.0 kg

#### Scenario: Invalid weight or date rejected
- **WHEN** the user saves a weight outside 20-400 kg or a date in the future
- **THEN** the check-in is rejected with a validation error and nothing is stored

### Requirement: REQ-002 Waist measured from the start
A new account SHALL start with one body part named "Talia" (registration language PL) or "Waist" (EN). It SHALL behave
like every other body part: the user can enter its value in cm in the weekly check-in, rename it and delete it, and can
add further body parts. Existing accounts SHALL NOT be changed.

#### Scenario: New account has a waist body part
- **WHEN** a user registers with language PL
- **THEN** their body part list contains exactly one part, "Talia"

#### Scenario: Waist value entered in the check-in
- **WHEN** the user saves a check-in with 80.5 cm for "Talia"
- **THEN** the progress view shows 80.5 cm for "Talia"

#### Scenario: Default waist can be deleted
- **WHEN** the user deletes "Talia" in Settings
- **THEN** it no longer appears in the check-in, on the progress screen or in the list of body parts

### Requirement: REQ-003 Weekly chart without moving average
The progress chart SHALL show at most one point per week for weight and for each body part, with no moving average.
A week's weight point SHALL be the average of all weight entries of that week (manual and synced), dated on the day of
the week's last entry. A body part's point SHALL be the week's check-in value.

#### Scenario: Several weights in one week give one point
- **WHEN** a week has weight entries of 68.0 kg on Wednesday and 69.0 kg on Friday
- **THEN** the chart has one point for that week with 68.5 kg dated on Friday

#### Scenario: One point per week in the chart
- **WHEN** the user has check-ins in three different weeks
- **THEN** the weight chart shows exactly three points

### Requirement: REQ-004 Difference since the first measurement
The progress screen SHALL show, for weight (kg) and for the headline body part (cm), the latest value and its
difference from the first value: latest weekly point minus first weekly point, one decimal, with a down arrow for less
and an up arrow for more. Without any value it SHALL show "no data".

#### Scenario: Weight lost since the start
- **WHEN** the first week's weight is 70.0 kg and the latest week's weight is 68.5 kg
- **THEN** the progress screen shows 68.5 kg and a difference of ▼ 1.5 kg

#### Scenario: Waist difference in cm
- **WHEN** the first waist value is 82.0 cm and the latest is 80.5 cm
- **THEN** the "Talia" tile shows 80.5 cm and ▼ 1.5 cm

#### Scenario: No measurements yet
- **WHEN** the user has no weight entries
- **THEN** the weight tile shows "no data" instead of a difference
