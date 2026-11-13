-- Steps entered manually once a week as the average daily steps (read from Apple Health).
-- Daily rows from the automatic sync stay in daily_steps; a weekly value takes precedence when present.
CREATE TABLE weekly_steps (
    week_start      DATE        PRIMARY KEY CHECK (extract(isodow FROM week_start) = 1),
    avg_daily_steps INTEGER     NOT NULL CHECK (avg_daily_steps BETWEEN 0 AND 100000),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
