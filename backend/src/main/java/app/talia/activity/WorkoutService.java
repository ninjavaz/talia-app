package app.talia.activity;

import app.talia.common.BadRequestException;
import app.talia.common.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class WorkoutService {

    public record Workout(long id, LocalDateTime startedAt, int durationMin, String type, WorkoutCategory category,
                          BigDecimal kcal) {
    }

    private static final LocalTime DEFAULT_PAST_TIME = LocalTime.of(12, 0);

    private final JdbcClient jdbc;
    private final Clock clock;

    WorkoutService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public List<Workout> between(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        SELECT id, started_at, ended_at, type, category, kcal FROM workout
                        WHERE started_at >= :from AND started_at < :next ORDER BY started_at
                        """)
                .param("from", from.atStartOfDay())
                .param("next", to.plusDays(1).atStartOfDay())
                .query((rs, n) -> {
                    LocalDateTime start = rs.getObject(2, LocalDateTime.class);
                    LocalDateTime end = rs.getObject(3, LocalDateTime.class);
                    return new Workout(rs.getLong(1), start, (int) Duration.between(start, end).toMinutes(),
                            rs.getString(4), WorkoutCategory.valueOf(rs.getString(5)), rs.getBigDecimal(6));
                })
                .list();
    }

    /**
     * Manual workout. Only the day matters to the user; the start time is "now" for today and noon for past days,
     * shifted by a minute if a workout of the same type already starts then (unique key start+type).
     */
    Workout add(LocalDate date, WorkoutCategory category, int durationMin) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            throw new BadRequestException("Date is in the future");
        }
        LocalTime time = date.equals(today)
                ? LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
                : DEFAULT_PAST_TIME;
        LocalDateTime start = date.atTime(time);
        for (int attempt = 0; attempt < 60; attempt++, start = start.plusMinutes(1)) {
            var id = jdbc.sql("""
                            INSERT INTO workout (started_at, ended_at, type, category)
                            VALUES (:start, :end, :type, :category)
                            ON CONFLICT (started_at, type) DO NOTHING
                            RETURNING id
                            """)
                    .param("start", start)
                    .param("end", start.plusMinutes(durationMin))
                    .param("type", category.name())
                    .param("category", category.name())
                    .query(Long.class)
                    .optional();
            if (id.isPresent()) {
                return new Workout(id.get(), start, durationMin, category.name(), category, null);
            }
        }
        throw new BadRequestException("Too many workouts of this type on that day");
    }

    void delete(long id) {
        if (jdbc.sql("DELETE FROM workout WHERE id = :id").param("id", id).update() == 0) {
            throw new NotFoundException("Workout not found");
        }
    }
}
