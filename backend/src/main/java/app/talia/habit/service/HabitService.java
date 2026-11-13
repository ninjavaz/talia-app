package app.talia.habit.service;

import app.talia.common.BadRequestException;
import app.talia.common.NotFoundException;
import app.talia.common.Weeks;
import app.talia.habit.model.Habit;
import app.talia.habit.model.HabitDay;
import app.talia.habit.model.HabitStats;
import app.talia.settings.service.SettingsService;
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
 * Daily habits of one user. Deleting a habit only deactivates it, so past checks stay in the history.
 * A week "counts" for the streak when at least {@code habit_week_threshold_pct} of the possible checks were done.
 * Every statement is scoped by user_id - a habit id of another user behaves exactly like a non-existing one.
 */
@Service
public class HabitService {

    private static final int MAX_STREAK_WEEKS = 104;

    private final JdbcClient jdbc;
    private final Clock clock;
    private final SettingsService settings;

    HabitService(JdbcClient jdbc, Clock clock, SettingsService settings) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.settings = settings;
    }

    public List<Habit> list(long userId) {
        return jdbc.sql("SELECT id, name, sort_order FROM habit WHERE user_id = :user AND active ORDER BY sort_order, id")
                .param("user", userId)
                .query((rs, n) -> new Habit(rs.getLong(1), rs.getString(2), rs.getInt(3)))
                .list();
    }

    @Transactional
    public Habit create(long userId, String name) {
        long id = jdbc.sql("""
                        INSERT INTO habit (user_id, name, sort_order)
                        VALUES (:user, :name,
                                (SELECT COALESCE(max(sort_order), 0) + 1 FROM habit WHERE user_id = :user AND active))
                        RETURNING id
                        """)
                .param("user", userId)
                .param("name", name.trim())
                .query(Long.class)
                .single();
        return find(userId, id);
    }

    public Habit rename(long userId, long id, String name) {
        int updated = jdbc.sql("UPDATE habit SET name = :name WHERE id = :id AND user_id = :user AND active")
                .param("name", name.trim()).param("id", id).param("user", userId).update();
        if (updated == 0) {
            throw new NotFoundException("Habit not found");
        }
        return find(userId, id);
    }

    public void delete(long userId, long id) {
        int updated = jdbc.sql("UPDATE habit SET active = FALSE WHERE id = :id AND user_id = :user AND active")
                .param("id", id).param("user", userId).update();
        if (updated == 0) {
            throw new NotFoundException("Habit not found");
        }
    }

    public List<HabitDay> day(long userId, LocalDate date) {
        return jdbc.sql("""
                        SELECT h.id, h.name, EXISTS (SELECT 1 FROM habit_check c WHERE c.habit_id = h.id AND c.day = :day)
                        FROM habit h WHERE h.user_id = :user AND h.active ORDER BY h.sort_order, h.id
                        """)
                .param("day", date)
                .param("user", userId)
                .query((rs, n) -> new HabitDay(rs.getLong(1), rs.getString(2), rs.getBoolean(3)))
                .list();
    }

    public void setDone(long userId, long id, LocalDate date, boolean done) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date is in the future");
        }
        if (!exists(userId, id)) {
            throw new NotFoundException("Habit not found");
        }
        if (done) {
            jdbc.sql("INSERT INTO habit_check (habit_id, day) VALUES (:id, :day) ON CONFLICT DO NOTHING")
                    .param("id", id).param("day", date).update();
        } else {
            jdbc.sql("DELETE FROM habit_check WHERE habit_id = :id AND day = :day")
                    .param("id", id).param("day", date).update();
        }
    }

    public HabitStats stats(long userId, LocalDate date) {
        int threshold = settings.get(userId).habitWeekThresholdPct();
        int total = jdbc.sql("SELECT count(*) FROM habit WHERE user_id = :user AND active")
                .param("user", userId).query(Integer.class).single();
        int doneToday = jdbc.sql("""
                        SELECT count(*) FROM habit_check c
                        JOIN habit h ON h.id = c.habit_id AND h.active AND h.user_id = :user
                        WHERE c.day = :day
                        """)
                .param("user", userId).param("day", date).query(Integer.class).single();

        // checks per week (Monday), active habits only, up to the given date
        Map<LocalDate, Integer> checksPerWeek = jdbc.sql("""
                        SELECT date_trunc('week', c.day)::date, count(*)
                        FROM habit_check c JOIN habit h ON h.id = c.habit_id AND h.active AND h.user_id = :user
                        WHERE c.day <= :day GROUP BY 1
                        """)
                .param("user", userId)
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

    private boolean exists(long userId, long id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM habit WHERE id = :id AND user_id = :user AND active)")
                .param("id", id).param("user", userId).query(Boolean.class).single();
    }

    private Habit find(long userId, long id) {
        return jdbc.sql("SELECT id, name, sort_order FROM habit WHERE id = :id AND user_id = :user")
                .param("id", id).param("user", userId)
                .query((rs, n) -> new Habit(rs.getLong(1), rs.getString(2), rs.getInt(3)))
                .optional()
                .orElseThrow(() -> new NotFoundException("Habit not found"));
    }
}
