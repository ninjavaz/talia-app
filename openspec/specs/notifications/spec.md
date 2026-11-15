# notifications Specification

## Purpose
Sends Web Push reminders to the user's devices so habits, the weekly check-in and the streak are not forgotten, and
lets the user control which reminders they get.

## Requirements

### Requirement: REQ-030 Habit reminder at its time
For every habit with a time, the user's devices SHALL get a push notification "Pora na: <habit>" at that time (in the
app's time zone) when the habit is not checked yet that day and habit reminders are enabled. Each habit SHALL be
reminded at most once per day. A reminder more than 30 minutes late (e.g. after a server restart) SHALL be skipped.

#### Scenario: Reminder at the habit time
- **WHEN** it is 10:00 and "Witamina D" has the time 10:00 and is not checked today
- **THEN** a notification "Pora na: Witamina D" is sent once

#### Scenario: Habit already done
- **WHEN** the habit is already checked today at its time
- **THEN** no notification is sent

#### Scenario: Reminders disabled
- **WHEN** habit reminders are switched off
- **THEN** no habit notification is sent

#### Scenario: Too late
- **WHEN** the habit's time was more than 30 minutes ago and no reminder was sent
- **THEN** no notification is sent for it today

### Requirement: REQ-031 Weekly measurement reminder
The user's devices SHALL get a push notification to do the weekly check-in on the configured day and time (default
Sunday 10:00) when the check-in of the current week is not done yet and the reminder is enabled; at most once per week.

#### Scenario: Check-in due
- **WHEN** it is Sunday 10:00 and this week has no check-in
- **THEN** a notification about the weekly measurement is sent once

#### Scenario: Check-in already done
- **WHEN** the check-in of this week is already saved
- **THEN** no notification is sent

#### Scenario: Other day configured
- **WHEN** the reminder is set to Saturday 09:00
- **THEN** no notification is sent on Sunday 10:00

### Requirement: REQ-033 Notification settings
The user SHALL be able to enable push on the current device (asking the browser for permission), disable it, send a
test notification, and switch each reminder type on and off. The reminder day (Monday-Sunday) and the times (HH:mm)
SHALL be configurable; invalid values SHALL be rejected. Push subscriptions SHALL be stored per user and device, SHALL
only be accepted for the known push services over HTTPS, and SHALL be removed when the push service reports them gone.

#### Scenario: Device subscribed
- **WHEN** the user enables notifications on a device and allows them
- **THEN** the device's subscription is stored for the user and a test notification can be sent to it

#### Scenario: Unknown push endpoint rejected
- **WHEN** a subscription with an endpoint outside the known push services or without HTTPS is sent
- **THEN** it is rejected

#### Scenario: Reminder type switched off
- **WHEN** the user switches off the weekly measurement reminder
- **THEN** the setting is saved and no such reminder is sent

#### Scenario: Gone subscription removed
- **WHEN** a push service answers that a subscription no longer exists
- **THEN** the subscription is deleted

### Requirement: REQ-034 Streak at risk in the evening
At the configured evening time (default 20:00) the user's devices SHALL get a push notification when the user has
habits and today is not completed yet (REQ-015), showing how many habits are left and the current streak; at most once
per day and only when this reminder is enabled.

#### Scenario: Day not completed
- **WHEN** it is 20:00 and one of three habits is checked today
- **THEN** a notification says that 2 of 3 habits are left

#### Scenario: Day completed
- **WHEN** all habits are checked at 20:00
- **THEN** no notification is sent
