# home-screen Specification

## Purpose
Defines the screen the app opens on and keeps every daily entry within two taps, because slow data entry is the main
reason to stop using the app.

## Requirements

### Requirement: REQ-052 Daily entries within two taps
After login and when the installed app is opened, the user SHALL land on the Week screen, which shows today's
progress, today's habits and the streak first and the week's workouts below. Every daily entry SHALL take at most two
taps from that screen: checking a habit one tap, adding a workout two taps (open the form, save with the prefilled
defaults: today, the default type and duration).

#### Scenario: App opens on the Week screen
- **WHEN** a logged-in user opens the app
- **THEN** the Week screen with today's habits is shown

#### Scenario: Habit in one tap
- **WHEN** the user taps a habit on the start screen
- **THEN** it is checked and today's progress is updated

#### Scenario: Workout in two taps
- **WHEN** the user taps "Add workout" and then "Save" without changing anything
- **THEN** a workout for today is stored and counted towards the weekly goal
