package app.talia.habit;

import app.talia.common.BadRequestException;
import app.talia.common.NotFoundException;
import app.talia.common.Weeks;
import app.talia.settings.SettingsRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Daily habits. Deleting a habit only deactivates it, so past checks stay in the history.
 * A week "counts" for the streak when at least {@code habit_week_threshold_pct} of the possible checks were done.
 */
@Service
public class HabitService {

    public record Habit(long id, String name, int sortOrder) {
    }

    public record HabitDay(long id, String name, boolean done) {
    }

    /** donePct: share of possible checks done this week so far; streakWeeks: weeks in a row meeting the threshold. */
    public record HabitStats(int doneToday, int total, int weekPct, int thresholdPct, int streakWeeks) {
    }

    private static final int MAX_STREAK_WEEKS = 104;

    private final JdbcClient jdbc;
    private final Clock clock;
    private final SettingsRepository settings;

    HabitService(JdbcClient jdbc, Clock clock, SettingsRepository settings) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.settings = settings;
    }

    List<Habit> list() {
        return jdbc.sql("SELECT id, name, sort_order FROM habit WHERE active ORDER BY sort_order, id")
                .query((rs, n) -> new Habit(rs.getLong(1), rs.getString(2), rs.getInt(3)))
                .list();
    }

    @Transactional
    Habit create(String name) {
        long id = jdbc.sql("""
                        INSERT INTO habit (name, sort_order)
                        VALUES (:name, (SELECT COALESCE(max(sort_order), 0) + 1 FROM habit WHERE active))
                        RETURNING id
                        """)
                .param("name", name.trim())
                .query(Long.class)
                .single();
        return find(id);
    }

    Habit rename(long id, String name) {
        int updated = jdbc.sql("UPDATE habit SET name = :name WHERE id = :id AND active")
                .param("name", name.trim()).param("id", id).update();
        if (updated == 0) {
            throw new NotFoundException("Habit not found");
        }
        return find(id);
    }

    void delete(long id) {
        int updated = jdbc.sql("UPDATE habit SET active = FALSE WHERE id = :id AND active").param("id", id).update();
        if (updated == 0) {
            throw new NotFoundException("Habit not found");
        }
    }

    List<HabitDay> day(LocalDate date) {
        return jdbc.sql("""
                        SELECT h.id, h.name, EXISTS (SELECT 1 FROM habit_check c WHERE c.habit_id = h.id AND c.day = :day)
                        FROM habit h WHERE h.active ORDER BY h.sort_order, h.id
                        """)
                .param("day", date)
                .query((rs, n) -> new HabitDay(rs.getLong(1), rs.getString(2), rs.getBoolean(3)))
                .list();
    }

    void setDone(long id, LocalDate date, boolean done) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date is in the future");
        }
        if (done) {
            int updated = jdbc.sql("""
                            INSERT INTO habit_check (habit_id, day)
                            SELECT id, :day FROM habit WHERE id = :id AND active
                            ON CONFLICT DO NOTHING
                            """)
                    .param("id", id).param("day", date).update();
            if (updated == 0 && !exists(id)) {
                throw new NotFoundException("Habit not found");
            }
        } else {
            jdbc.sql("DELETE FROM habit_check WHERE habit_id = :id AND day = :day")
                    .param("id", id).param("day", date).update();
        }
    }

    public HabitStats stats(LocalDate date) {
        int threshold = settings.get().habitWeekThresholdPct();
        int total = jdbc.sql("SELECT count(*) FROM habit WHERE active").query(Integer.class).single();
        int doneToday = jdbc.sql("""
                        SELECT count(*) FROM habit_check c JOIN habit h ON h.id = c.habit_id AND h.active
                        WHERE c.day = :day
                        """)
                .param("day", date).query(Integer.class).single();

        // checks per week (Monday), active habits only, up to the given date
        Map<LocalDate, Integer> checksPerWeek = jdbc.sql("""
                        SELECT date_trunc('week', c.day)::date, count(*)
                        FROM habit_check c JOIN habit h ON h.id = c.habit_id AND h.active
                        WHERE c.day <= :day GROUP BY 1
                        """)
                .param("day", date)
                .query((rs, n) -> Map.entry(rs.getObject(1, LocalDate.class), rs.getInt(2)))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        LocalDate thisWeek = Weeks.start(date);
        int daysSoFar = (int) ChronoUnit.DAYS.between(thisWeek, date) + 1;
        int weekPct = percent(checksPerWeek.getOrDefault(thisWeek, 0), total * daysSoFar);

        // The running week counts once it already meets the threshold; if not (yet), it doesn't break the streak.
        int streak = weekPct >= threshold && total > 0 ? 1 : 0;
        LocalDate week = thisWeek.minusWeeks(1);
        while (streak < MAX_STREAK_WEEKS && total > 0
                && percent(checksPerWeek.getOrDefault(week, 0), total * 7) >= threshold) {
            streak++;
            week = week.minusWeeks(1);
        }
        return new HabitStats(doneToday, total, weekPct, threshold, streak);
    }

    private static int percent(int done, int possible) {
        return possible <= 0 ? 0 : Math.min(100, Math.round(done * 100f / possible));
    }

    private boolean exists(long id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM habit WHERE id = :id AND active)")
                .param("id", id).query(Boolean.class).single();
    }

    private Habit find(long id) {
        return jdbc.sql("SELECT id, name, sort_order FROM habit WHERE id = :id")
                .param("id", id)
                .query((rs, n) -> new Habit(rs.getLong(1), rs.getString(2), rs.getInt(3)))
                .optional()
                .orElseThrow(() -> new NotFoundException("Habit not found"));
    }
}
