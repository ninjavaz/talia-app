package app.talia.sync;

import app.talia.activity.WorkoutCategory;
import app.talia.config.TaliaProperties;
import app.talia.sync.HealthSyncResponse.Counts;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Idempotent upserts of Apple Health data. A row is only touched when a value actually changed;
 * {@code RETURNING (xmax = 0)} tells whether it was inserted (true) or updated (false), and no returned row
 * means the data was already there.
 */
@Service
class HealthSyncService {

    private static final String UPSERT_STEPS = """
            INSERT INTO daily_steps (user_id, day, steps) VALUES (:user, :day, :steps)
            ON CONFLICT (user_id, day) DO UPDATE SET steps = EXCLUDED.steps, updated_at = now()
            WHERE daily_steps.steps IS DISTINCT FROM EXCLUDED.steps
            RETURNING (xmax = 0)
            """;

    private static final String UPSERT_WEIGHT = """
            INSERT INTO weight_entry (user_id, measured_at, value_kg, source) VALUES (:user, :ts, :value, 'HEALTH')
            ON CONFLICT (user_id, measured_at) DO UPDATE SET value_kg = EXCLUDED.value_kg, updated_at = now()
            WHERE weight_entry.value_kg IS DISTINCT FROM EXCLUDED.value_kg
            RETURNING (xmax = 0)
            """;

    private static final String UPSERT_WORKOUT = """
            INSERT INTO workout (user_id, started_at, ended_at, type, category, kcal)
            VALUES (:user, :start, :end, :type, :category, :kcal)
            ON CONFLICT (user_id, started_at, type) DO UPDATE
               SET ended_at = EXCLUDED.ended_at, category = EXCLUDED.category, kcal = EXCLUDED.kcal, updated_at = now()
            WHERE (workout.ended_at, workout.category, workout.kcal)
                  IS DISTINCT FROM (EXCLUDED.ended_at, EXCLUDED.category, EXCLUDED.kcal)
            RETURNING (xmax = 0)
            """;

    private final JdbcClient jdbc;
    private final ZoneId zone;

    HealthSyncService(JdbcClient jdbc, TaliaProperties properties) {
        this.jdbc = jdbc;
        this.zone = properties.timezone();
    }

    @Transactional
    HealthSyncResponse sync(long userId, HealthSyncRequest request) {
        Tally steps = new Tally();
        for (var s : request.steps()) {
            steps.add(upsert(jdbc.sql(UPSERT_STEPS).param("user", userId).param("day", s.date()).param("steps", s.value())));
        }

        Tally weight = new Tally();
        for (var w : request.weight()) {
            LocalDateTime ts = FlexibleDateTime.parse(w.ts(), zone);
            weight.add(upsert(jdbc.sql(UPSERT_WEIGHT).param("user", userId).param("ts", ts).param("value", w.value())));
        }

        Tally workouts = new Tally();
        for (var w : request.workouts()) {
            LocalDateTime start = FlexibleDateTime.parse(w.start(), zone);
            LocalDateTime end = FlexibleDateTime.parse(w.end(), zone);
            if (end.isBefore(start)) {
                throw new InvalidSyncDataException("Workout ends before it starts: " + w.start());
            }
            String type = w.type().trim();
            workouts.add(upsert(jdbc.sql(UPSERT_WORKOUT)
                    .param("user", userId)
                    .param("start", start)
                    .param("end", end)
                    .param("type", type)
                    .param("category", WorkoutCategory.fromHealthType(type).name())
                    .param("kcal", w.kcal())));
        }

        jdbc.sql("INSERT INTO sync_log (user_id, inserted, updated) VALUES (:user, :inserted, :updated)")
                .param("user", userId)
                .param("inserted", steps.inserted + weight.inserted + workouts.inserted)
                .param("updated", steps.updated + weight.updated + workouts.updated)
                .update();

        return new HealthSyncResponse(steps.toCounts(), weight.toCounts(), workouts.toCounts());
    }

    private static Optional<Boolean> upsert(JdbcClient.StatementSpec statement) {
        return statement.query((rs, rowNum) -> rs.getBoolean(1)).optional();
    }

    private static final class Tally {
        int inserted;
        int updated;
        int unchanged;

        void add(Optional<Boolean> result) {
            if (result.isEmpty()) {
                unchanged++;
            } else if (result.get()) {
                inserted++;
            } else {
                updated++;
            }
        }

        Counts toCounts() {
            return new Counts(inserted, updated, unchanged);
        }
    }
}
