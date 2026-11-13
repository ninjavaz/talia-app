-- Talia schema. Every piece of personal data belongs to an app_user and is deleted together with the account.

-- Users ---------------------------------------------------------------------

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(30)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    -- SHA-256 (hex) of the personal Apple Health sync token; the token itself is never stored
    sync_token_hash CHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    onboarded_at    TIMESTAMPTZ,
    CONSTRAINT app_user_username_uk UNIQUE (username),
    CONSTRAINT app_user_sync_token_uk UNIQUE (sync_token_hash),
    CONSTRAINT app_user_username_lower_ck CHECK (username = lower(username))
);

CREATE TABLE user_settings (
    user_id                  BIGINT     PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    weekly_workout_goal      INTEGER    NOT NULL DEFAULT 3    CHECK (weekly_workout_goal BETWEEN 0 AND 21),
    daily_steps_goal         INTEGER    NOT NULL DEFAULT 8000 CHECK (daily_steps_goal BETWEEN 0 AND 100000),
    habit_week_threshold_pct INTEGER    NOT NULL DEFAULT 70   CHECK (habit_week_threshold_pct BETWEEN 1 AND 100),
    language                 VARCHAR(2) NOT NULL DEFAULT 'pl' CHECK (language IN ('pl', 'en'))
);

-- Measurements --------------------------------------------------------------

CREATE TABLE weight_entry (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    measured_at TIMESTAMP    NOT NULL,
    value_kg    NUMERIC(5,2) NOT NULL CHECK (value_kg > 0),
    source      VARCHAR(10)  NOT NULL CHECK (source IN ('HEALTH', 'MANUAL')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT weight_entry_user_measured_at_uk UNIQUE (user_id, measured_at)
);

-- Body measurements (waist, biceps, thigh...). code 'WAIST' = built-in waist, one per user, not deletable.
CREATE TABLE body_part (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name       VARCHAR(50) NOT NULL,
    code       VARCHAR(20),
    sort_order INTEGER     NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT body_part_user_code_uk UNIQUE (user_id, code)
);

CREATE INDEX body_part_user_idx ON body_part (user_id);

CREATE TABLE body_measurement (
    id           BIGSERIAL PRIMARY KEY,
    body_part_id BIGINT       NOT NULL REFERENCES body_part (id) ON DELETE CASCADE,
    measured_on  DATE         NOT NULL,
    value_cm     NUMERIC(5,1) NOT NULL CHECK (value_cm > 0),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT body_measurement_part_day_uk UNIQUE (body_part_id, measured_on)
);

-- Activity ------------------------------------------------------------------

CREATE TABLE daily_steps (
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    day        DATE        NOT NULL,
    steps      INTEGER     NOT NULL CHECK (steps >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, day)
);

-- Average daily steps entered once a week; wins over the synced daily values of that week.
CREATE TABLE weekly_steps (
    user_id         BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    week_start      DATE        NOT NULL CHECK (extract(isodow FROM week_start) = 1),
    avg_daily_steps INTEGER     NOT NULL CHECK (avg_daily_steps BETWEEN 0 AND 100000),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, week_start)
);

CREATE TABLE workout (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    started_at TIMESTAMP    NOT NULL,
    ended_at   TIMESTAMP    NOT NULL,
    type       VARCHAR(100) NOT NULL,
    category   VARCHAR(10)  NOT NULL CHECK (category IN ('GYM', 'PILATES', 'WALK', 'OTHER')),
    kcal       NUMERIC(7,1),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT workout_user_start_type_uk UNIQUE (user_id, started_at, type),
    CONSTRAINT workout_end_after_start_ck CHECK (ended_at >= started_at)
);

CREATE TABLE sync_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    inserted    INTEGER     NOT NULL,
    updated     INTEGER     NOT NULL
);

CREATE INDEX sync_log_user_idx ON sync_log (user_id, received_at);

-- Habits --------------------------------------------------------------------

CREATE TABLE habit (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX habit_user_idx ON habit (user_id);

CREATE TABLE habit_check (
    habit_id   BIGINT      NOT NULL REFERENCES habit (id) ON DELETE CASCADE,
    day        DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (habit_id, day)
);

-- Web Push ------------------------------------------------------------------

CREATE TABLE push_subscription (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    endpoint   TEXT        NOT NULL,
    p256dh     TEXT        NOT NULL,
    auth       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT push_subscription_endpoint_uk UNIQUE (endpoint)
);

-- Spring Session JDBC (schema-postgresql.sql) ---------------------------------

CREATE TABLE spring_session (
    primary_id            CHAR(36) NOT NULL,
    session_id            CHAR(36) NOT NULL,
    creation_time         BIGINT   NOT NULL,
    last_access_time      BIGINT   NOT NULL,
    max_inactive_interval INT      NOT NULL,
    expiry_time           BIGINT   NOT NULL,
    principal_name        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
