# Tasks

## 1. Database

- [x] 1.1 Migration `V5__push.sql` (reminder settings, `push_sent`, `push_subscription.last_used_at`) and settings entity/model fields with validation; verify the backend suite passes

## 2. Push infrastructure (tests first)

- [x] 2.1 `WebPushCryptoTest` (RFC 8291 test vector) and `VapidTest` (JWT verifies with the public key, claims) first, then `WebPushCrypto` and `Vapid`; verify both pass
- [ ] 2.2 Subscription API tests "REQ-033 ..." (subscribe, update, unsubscribe, unknown host/http rejected, limit, login required, isolation) and a `WebPushClient` test against a local HTTP server (headers, body size, 410 deletes); then `PushController`, `PushService`, `WebPushClient`; verify all backend tests pass

## 3. Reminders (tests first)

- [ ] 3.1 `PushReminderIntegrationTest` "REQ-030 ...", "REQ-031 ...", "REQ-034 ..." and "REQ-033 settings ..." with a mocked `PushSender`; then `PushReminderService` and the scheduler; verify all backend tests pass

## 4. Frontend

- [ ] 4.1 `core/push.ts`, Settings section "Powiadomienia" (device, test, switches, day/time), translations PL/EN, `scripts/vapid-keys.cmd`, README; verify `ng build` and `ng test` pass

## 5. E2e (Playwright)

- [ ] 5.1 `frontend/e2e/notifications.e2e.ts`: "REQ-033 reminder switches, day and time are saved" and "REQ-033 device push needs the installed app" (dev server has no service worker); verify `npm run e2e` passes

## 6. Wrap-up

- [ ] 6.1 Update CLAUDE.md, list assumptions in design.md, full build (backend tests, `ng build`, `ng test`, `npm run e2e`), archive the change in the same PR
