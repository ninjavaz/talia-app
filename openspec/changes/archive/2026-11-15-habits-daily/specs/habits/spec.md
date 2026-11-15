# Spec Delta

## Purpose

Lets the user keep a short list of daily habits, check them off each day, see today's progress and keep a streak of
fully completed days.

## ADDED Requirements

### Requirement: REQ-010 Configurable habit list
The user SHALL be able to add a habit (name 1-100 characters), edit its name and time, delete it and change the order
of the list. The order SHALL be the same everywhere the habits are listed. Deleting a habit SHALL hide it from the
lists while its past checks stay stored. A new account SHALL start with three habits in this order: "Suplementy",
"Picie wody" and the steps habit (EN: "Supplements", "Drinking water", steps habit), none with a time.
Habits of another user SHALL behave like non-existing ones.

#### Scenario: Default habits of a new account
- **WHEN** a user registers with language PL
- **THEN** their habit list is "Suplementy", "Picie wody" and the steps habit, without times

#### Scenario: Add, edit and delete
- **WHEN** the user adds "Witamina D", renames it to "Witamina D3" and deletes "Picie wody"
- **THEN** the list contains "Suplementy", the steps habit and "Witamina D3"

#### Scenario: Reorder
- **WHEN** the user moves the steps habit to the top
- **THEN** the steps habit is listed first in Settings and on the daily list

#### Scenario: Reorder with an incomplete or foreign list
- **WHEN** a reorder request does not contain exactly the user's own habits
- **THEN** it is rejected and the order does not change

### Requirement: REQ-011 Optional time of a habit
A habit SHALL have an optional time of day (hours and minutes). The daily list SHALL show the time next to the habit.
Several habits MAY share a name pattern with different times (e.g. one habit per supplement).

#### Scenario: Habit with a time
- **WHEN** the user adds "Witamina D" with time 08:00
- **THEN** the daily list shows "Witamina D" with "8:00"

#### Scenario: Time removed
- **WHEN** the user clears the time of "Witamina D"
- **THEN** the habit has no time

#### Scenario: Invalid time
- **WHEN** a habit is saved with a time that is not a valid time of day
- **THEN** it is rejected

### Requirement: REQ-012 One-tap daily check
Each habit on the daily list SHALL be checked and unchecked for today with a single tap. Checking twice SHALL have the
same effect as once. Days in the future SHALL NOT be checkable.

#### Scenario: Check and uncheck
- **WHEN** the user taps an unchecked habit and then taps it again
- **THEN** it is checked after the first tap and unchecked after the second

#### Scenario: Future day
- **WHEN** a check for tomorrow is sent
- **THEN** it is rejected

### Requirement: REQ-013 Steps as a habit
A user SHALL have at most one steps habit. It SHALL be shown as "Kroki: <goal>" (EN "Steps: <goal>") with the daily
steps goal from Settings and SHALL be checked by hand like any habit (no automatic source). Its name SHALL follow the
goal and SHALL NOT be editable; its time, order and deletion work like for other habits. When the user has no steps
habit, Settings SHALL offer to add it.

#### Scenario: Steps habit follows the goal
- **WHEN** the user changes the daily steps goal to 10 000
- **THEN** the steps habit is shown as "Kroki: 10 000"

#### Scenario: Only one steps habit
- **WHEN** the user tries to add a second steps habit
- **THEN** it is rejected

#### Scenario: Steps habit checked by hand
- **WHEN** the user taps the steps habit
- **THEN** it is checked for today like any other habit

### Requirement: REQ-014 Daily progress
The daily list SHALL show a progress bar with the number of habits checked today out of all current habits.

#### Scenario: Two of three done
- **WHEN** two of three habits are checked today
- **THEN** the progress shows 2 / 3

### Requirement: REQ-015 Streak of completed days
A day SHALL count as completed when every habit that was active on that day (added on or before that day and not
deleted before or on that day) is checked; a day without active habits SHALL NOT count. The streak SHALL be the number
of consecutive completed days ending today when today is completed, otherwise ending yesterday (an incomplete today
never breaks the streak). Adding a habit SHALL NOT change whether earlier days were completed. The rule SHALL be
implemented in one place.

#### Scenario: Consecutive completed days
- **WHEN** all habits were checked on each of the last three days before today, but not four days ago, and today is not complete yet
- **THEN** the streak is 3 days

#### Scenario: Today completes
- **WHEN** in the same situation the user checks the last habit of today
- **THEN** the streak is 4 days

#### Scenario: New habit does not break the past
- **WHEN** the user adds a new habit today after three completed days
- **THEN** the streak is still 3 days

#### Scenario: Missed day breaks the streak
- **WHEN** yesterday one habit was not checked
- **THEN** the streak counts only today (1 if today is complete, otherwise 0)
