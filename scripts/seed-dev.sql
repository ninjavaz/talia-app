-- Sample data for local development: ~8 weeks of a slow, realistic downward trend.
-- Run via scripts\db-seed.cmd. Replaces measurements, activity and habit checks; keeps habits and settings.

DO $$
BEGIN
    IF to_regclass('public.weight_entry') IS NULL THEN
        RAISE EXCEPTION 'Brak schematu - uruchom najpierw backend (Flyway tworzy tabele).';
    END IF;
END $$;

SELECT setseed(0.42);

TRUNCATE weight_entry, body_measurement, weekly_steps, daily_steps, workout, habit_check, sync_log;

-- Extra body parts for the demo (added once).
INSERT INTO body_part (name, sort_order)
SELECT v.name, v.ord FROM (VALUES ('Biceps', 1), ('Udo', 2)) AS v(name, ord)
WHERE NOT EXISTS (SELECT 1 FROM body_part p WHERE p.name = v.name AND p.active);

UPDATE app_settings SET start_date = current_date - 56;

-- Weight: almost every morning, -0.035 kg/day with +-0.6 kg daily noise (water, salt, cycle).
INSERT INTO weight_entry (measured_at, value_kg, source)
SELECT (current_date - d) + time '06:45' + (random() * interval '50 minutes'),
       round((68.5 + 0.035 * d + (random() - 0.5) * 1.2)::numeric, 1),
       'HEALTH'
FROM generate_series(0, 56) AS d
WHERE random() > 0.12;

-- Body measurements: once a week (Sundays). Waist -0.4 cm/week, thigh -0.3, biceps almost flat.
INSERT INTO body_measurement (body_part_id, measured_on, value_cm)
SELECT p.id, current_date - w * 7,
       round((CASE p.code WHEN 'WAIST' THEN 78.0 + 0.4 * w ELSE
              CASE p.name WHEN 'Udo' THEN 56.0 + 0.3 * w ELSE 28.5 + 0.05 * w END END
              + (random() - 0.5) * 0.4)::numeric, 1)
FROM generate_series(0, 8) AS w
CROSS JOIN body_part p
WHERE p.active AND (p.code = 'WAIST' OR p.name IN ('Biceps', 'Udo'));

-- Steps: every day, today is partial.
INSERT INTO daily_steps (day, steps)
SELECT current_date - d,
       CASE WHEN d = 0 THEN 3200 + (random() * 2000)::int
            ELSE 4500 + (random() * 7500)::int END
FROM generate_series(0, 56) AS d;

-- Workouts: gym on Tuesdays, pilates on Thursdays, a long walk on Saturdays (sometimes skipped).
INSERT INTO workout (started_at, ended_at, type, category, kcal)
SELECT day + time '18:00', day + time '19:05', 'Traditional Strength Training', 'GYM', 280 + (random() * 80)::int
FROM (SELECT current_date - d AS day FROM generate_series(1, 56) AS d) AS g
WHERE extract(isodow FROM day) = 2 AND random() > 0.15;

INSERT INTO workout (started_at, ended_at, type, category, kcal)
SELECT day + time '19:00', day + time '19:55', 'Pilates', 'PILATES', 170 + (random() * 40)::int
FROM (SELECT current_date - d AS day FROM generate_series(1, 56) AS d) AS g
WHERE extract(isodow FROM day) = 4 AND random() > 0.2;

INSERT INTO workout (started_at, ended_at, type, category, kcal)
SELECT day + time '10:30', day + time '11:40', 'Walking', 'WALK', 220 + (random() * 60)::int
FROM (SELECT current_date - d AS day FROM generate_series(1, 56) AS d) AS g
WHERE extract(isodow FROM day) = 6 AND random() > 0.3;

-- Habits: each active habit done on ~75% of days (today left open to tap).
INSERT INTO habit_check (habit_id, day)
SELECT h.id, current_date - d
FROM habit h
CROSS JOIN generate_series(1, 56) AS d
WHERE h.active AND random() < 0.75;

INSERT INTO sync_log (received_at, inserted, updated) VALUES (now() - interval '2 hours', 3, 1);

SELECT 'weight' AS what, count(*) FROM weight_entry
UNION ALL SELECT 'body measurements', count(*) FROM body_measurement
UNION ALL SELECT 'steps', count(*) FROM daily_steps
UNION ALL SELECT 'workouts', count(*) FROM workout
UNION ALL SELECT 'habit checks', count(*) FROM habit_check;
