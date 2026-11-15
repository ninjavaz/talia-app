-- REQ-010..015: habit kind (steps habit), optional time, the days a habit was active (for the daily streak);
-- the weekly streak threshold is gone.

ALTER TABLE habit
    ADD COLUMN kind           VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (kind IN ('NORMAL', 'STEPS')),
    ADD COLUMN remind_at      TIME,
    ADD COLUMN created_on     DATE,
    ADD COLUMN deactivated_on DATE;

UPDATE habit SET created_on = (created_at AT TIME ZONE 'Europe/Warsaw')::date;
-- habits deleted before this migration have no known deletion day: treat them as never active
UPDATE habit SET deactivated_on = created_on WHERE NOT active;

ALTER TABLE habit
    ALTER COLUMN created_on SET NOT NULL,
    ADD CONSTRAINT habit_active_days_ck CHECK (deactivated_on IS NULL OR deactivated_on >= created_on);

-- at most one (active) steps habit per user
CREATE UNIQUE INDEX habit_one_steps_per_user_uk ON habit (user_id) WHERE kind = 'STEPS' AND active;

ALTER TABLE user_settings DROP COLUMN habit_week_threshold_pct;
