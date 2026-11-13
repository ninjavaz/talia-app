-- Measurements --------------------------------------------------------------

CREATE TABLE weight_entry (
    id          BIGSERIAL PRIMARY KEY,
    measured_at TIMESTAMP    NOT NULL,
    value_kg    NUMERIC(5,2) NOT NULL CHECK (value_kg > 0),
    source      VARCHAR(10)  NOT NULL CHECK (source IN ('HEALTH', 'MANUAL')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT weight_entry_measured_at_uk UNIQUE (measured_at)
);

CREATE TABLE waist_entry (
    id          BIGSERIAL PRIMARY KEY,
    measured_on DATE         NOT NULL,
    value_cm    NUMERIC(5,1) NOT NULL CHECK (value_cm > 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT waist_entry_measured_on_uk UNIQUE (measured_on)
);

-- Activity ------------------------------------------------------------------

CREATE TABLE daily_steps (
    day        DATE        PRIMARY KEY,
    steps      INTEGER     NOT NULL CHECK (steps >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workout (
    id         BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP    NOT NULL,
    ended_at   TIMESTAMP    NOT NULL,
    type       VARCHAR(100) NOT NULL,
    category   VARCHAR(10)  NOT NULL CHECK (category IN ('GYM', 'PILATES', 'WALK', 'OTHER')),
    kcal       NUMERIC(7,1),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT workout_start_type_uk UNIQUE (started_at, type),
    CONSTRAINT workout_end_after_start_ck CHECK (ended_at >= started_at)
);

CREATE TABLE sync_log (
    id          BIGSERIAL PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    inserted    INTEGER     NOT NULL,
    updated     INTEGER     NOT NULL
);

-- Habits --------------------------------------------------------------------

CREATE TABLE habit (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE habit_check (
    habit_id   BIGINT      NOT NULL REFERENCES habit (id) ON DELETE CASCADE,
    day        DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (habit_id, day)
);

INSERT INTO habit (name, sort_order) VALUES
    ('Białko w posiłkach', 1),
    ('Warzywa do 2 posiłków', 2),
    ('Bez słodkich napojów', 3);

-- Settings (exactly one row) -------------------------------------------------

CREATE TABLE app_settings (
    id                       SMALLINT   PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    start_date               DATE,
    weekly_workout_goal      INTEGER    NOT NULL DEFAULT 3    CHECK (weekly_workout_goal BETWEEN 0 AND 21),
    daily_steps_goal         INTEGER    NOT NULL DEFAULT 8000 CHECK (daily_steps_goal BETWEEN 0 AND 100000),
    habit_week_threshold_pct INTEGER    NOT NULL DEFAULT 70   CHECK (habit_week_threshold_pct BETWEEN 1 AND 100),
    language                 VARCHAR(2) NOT NULL DEFAULT 'pl' CHECK (language IN ('pl', 'en'))
);

INSERT INTO app_settings (id) VALUES (1);

-- Web Push ------------------------------------------------------------------

CREATE TABLE push_subscription (
    id         BIGSERIAL PRIMARY KEY,
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
