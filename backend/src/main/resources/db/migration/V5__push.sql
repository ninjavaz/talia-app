-- REQ-030..034: Web Push reminders.

ALTER TABLE user_settings
    ADD COLUMN push_habits           BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN push_checkin          BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN push_streak           BOOLEAN NOT NULL DEFAULT TRUE,
    -- ISO day of week, 1 = Monday ... 7 = Sunday
    ADD COLUMN checkin_reminder_day  INTEGER NOT NULL DEFAULT 7 CHECK (checkin_reminder_day BETWEEN 1 AND 7),
    ADD COLUMN checkin_reminder_time TIME    NOT NULL DEFAULT '10:00',
    ADD COLUMN streak_reminder_time  TIME    NOT NULL DEFAULT '20:00';

ALTER TABLE push_subscription ADD COLUMN last_used_at TIMESTAMPTZ;
CREATE INDEX push_subscription_user_idx ON push_subscription (user_id);

-- one row per reminder that was sent: de-duplication (inserted before sending, ON CONFLICT DO NOTHING)
CREATE TABLE push_sent (
    user_id BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    kind    VARCHAR(10) NOT NULL CHECK (kind IN ('HABIT', 'CHECKIN', 'STREAK')),
    -- the habit id for HABIT, 0 otherwise
    ref_id  BIGINT      NOT NULL,
    -- the day (HABIT, STREAK) or the Monday of the week (CHECKIN)
    day     DATE        NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, kind, ref_id, day)
);
