# Design

## Context

- `push_subscription (id, user_id, endpoint UNIQUE, p256dh, auth, created_at)` exists since V1 and is unused.
- The frontend registers the Angular service worker (`ngsw-worker.js`) only in production builds; `SwPush` shows
  notifications whose payload is `{"notification": {...}}` and handles clicks via `data.onActionClick`.
- Times are local `Europe/Warsaw` (`talia.timezone`), `Clock` is injectable (tests fix it to Sun 15 Nov 2026 10:00).

## Goals / Non-Goals

**Goals:**
- No third-party crypto library: JDK only (`ECDH`, `HmacSHA256`, `AES/GCM`, `SHA256withECDSAinP1363Format`), verified
  with the RFC 8291 test vector.
- Reminders are sent at most once per key, survive restarts, and never flood after downtime.
- No server-side request to an arbitrary URL (SSRF).

**Non-Goals:**
- REQ-032 summary push (v2), Health sync alert, quiet hours, per-device settings (settings are per user).

## Decisions

- **Package `push`**: `controller/PushController`, `service/PushService` (subscriptions, send to a user, remove gone
  ones), `service/PushReminderService` (rules for REQ-030/031/034, `@Scheduled` every minute), `service/WebPushClient`
  implements `PushSender` (HTTP), `service/WebPushCrypto` (RFC 8291 encryption, pure), `service/Vapid` (keys + JWT),
  `entity/PushSubscriptionEntity`, `entity/PushSentEntity`, `model/*`.
- **Keys**: `talia.push.vapid-public-key` / `vapid-private-key` (base64url, raw 65-byte point / 32-byte scalar) and
  `vapid-subject` from env. Missing keys: a random pair is generated at startup with a warning (local dev; devices
  re-subscribe on the next app start because the frontend compares the server key). `scripts/vapid-keys.cmd` prints a
  new pair using Node's `crypto` (no npm package).
- **Encryption**: `WebPushCrypto.encrypt(plaintext, uaPublic, authSecret, asKeyPair, salt)` is deterministic for the
  test vector; production uses a fresh ephemeral key pair and random salt per message; record size 4096, one record.
- **VAPID**: JWT `{"typ":"JWT","alg":"ES256"}` / `{"aud": origin of endpoint, "exp": now + 12 h, "sub": subject}`,
  header `Authorization: vapid t=<jwt>, k=<public key>`; plus `TTL` (habit 1 h, others 6 h), `Urgency: normal`,
  `Content-Encoding: aes128gcm`. Response 404/410 deletes the subscription; other errors are logged.
- **SSRF guard**: endpoints must be `https` with a host ending in one of `talia.push.allowed-host-suffixes` (default:
  `fcm.googleapis.com`, `push.services.mozilla.com`, `push.apple.com`, `notify.windows.com`); validated on subscribe
  and again before sending. Tests add `localhost` and allow `http` through test properties only. Max 10 subscriptions
  per user (oldest removed).
- **Scheduler**: `@Scheduled(cron = "0 * * * * *", zone = "${talia.timezone}")` calls `sendDue()`. For each user with
  a subscription and each rule, "due" means the rule time lies in `(now - 30 min, now]` today; de-duplication by
  inserting `push_sent (user_id, kind, ref_id, day)` with `ON CONFLICT DO NOTHING` first (sent only when inserted).
  Weekly check-in uses the week's Monday as `day`.
- **Settings** (V5, `user_settings`): `push_habits`, `push_checkin`, `push_streak` (boolean, default true),
  `checkin_reminder_day` (1-7, default 7), `checkin_reminder_time` (default 10:00), `streak_reminder_time` (default
  20:00). Part of `AppSettings` (`"HH:mm"`).
- **Texts**: built in the backend in the user's language (`user_settings.language`); payload in the `SwPush`
  notification format with `icon` and a click action opening `/week` (`/progress` for the check-in).
- **Frontend**: `core/push.ts` wraps `SwPush` (supported?, permission, enable/disable, re-sync on app start);
  Settings section with device status, enable/disable, test button, three switches (`tlCheck`), day (`tl-segmented`)
  and time inputs saved immediately. Where push is unavailable (dev server, iOS Safari tab) a hint explains how to
  install the app.

## Risks / Trade-offs

- [Own crypto code] → RFC test vector, JWT verified with the JDK, round-trip of the HTTP request against a local
  server in tests.
- [Real delivery cannot be tested in CI/e2e] → e2e covers settings only; delivery is tested manually on the phone
  after deployment (HTTPS needed).
- [Ephemeral dev keys] → only when env keys are missing; production must set them (documented in README).
