# Proposal

## Why

Reminders keep the daily and weekly entries going (REQ-030, REQ-031) and the evening nudge protects the streak.
The app is an installed PWA on iPhone, so Web Push is the only channel that works without a native app. Nothing is
implemented yet apart from an unused `push_subscription` table.

## What Changes

- Web Push infrastructure: VAPID keys from the environment, a push client implemented with the JDK (RFC 8291 message
  encryption, RFC 8292 VAPID), subscriptions per device, a once-a-minute scheduler with de-duplication.
  Push endpoints are restricted to the known push services over HTTPS (no server-side requests to arbitrary URLs).
- REQ-030: a reminder at the time of a habit (REQ-011) when the habit is not checked yet that day.
- REQ-031: a weekly reminder to measure weight and body (default Sunday 10:00, day and time configurable) when the
  week's check-in is not done yet.
- **REQ-034 (new ID for the agreed "streak at risk" push)**: an evening reminder (default 20:00, configurable) when
  today is not completed yet.
- REQ-033: in Settings, enable/disable push on this device, a test notification, and a switch per reminder type.
- REQ-032 ("summary ready") is **not** part of this change: it comes with the AI summary in v2.
- Not done: the "no health sync for 48 h" alert from CLAUDE.md (Apple Health is out of scope for now).

## Capabilities

### New Capabilities
- `notifications`: push subscriptions, reminder rules and their settings.

### Modified Capabilities
<!-- none -->

## Impact

- Database: migration V5 (reminder settings in `user_settings`, `push_sent` for de-duplication, `push_subscription`
  gets `last_used_at`).
- Backend: new `push` package (controller, service, client, scheduler, models), `settings` (new fields), config
  `talia.push.*` and env `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT`.
- API: `GET /api/push/key`, `PUT /api/push/subscriptions`, `DELETE /api/push/subscriptions`, `POST /api/push/test`;
  `AppSettings` gets the reminder fields.
- Frontend: push service (Angular `SwPush`), Settings section "Powiadomienia", translations PL/EN, a key generator
  script for VAPID keys (`scripts/vapid-keys.cmd`, Node crypto only).
- Data sent to push services: only the encrypted notification text; push services see the endpoint and timing.
