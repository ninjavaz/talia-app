-- Custom body measurements (waist, biceps, thigh...). Waist becomes the built-in part with code 'WAIST'
-- (it cannot be deleted and is the headline metric of the app).
CREATE TABLE body_part (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    code       VARCHAR(20) UNIQUE,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO body_part (name, code, sort_order) VALUES ('Talia', 'WAIST', 0);

CREATE TABLE body_measurement (
    id           BIGSERIAL PRIMARY KEY,
    body_part_id BIGINT       NOT NULL REFERENCES body_part (id) ON DELETE CASCADE,
    measured_on  DATE         NOT NULL,
    value_cm     NUMERIC(5,1) NOT NULL CHECK (value_cm > 0),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT body_measurement_part_day_uk UNIQUE (body_part_id, measured_on)
);

CREATE INDEX body_measurement_day_idx ON body_measurement (measured_on);

INSERT INTO body_measurement (body_part_id, measured_on, value_cm, created_at)
SELECT (SELECT id FROM body_part WHERE code = 'WAIST'), measured_on, value_cm, created_at
FROM waist_entry;

DROP TABLE waist_entry;
