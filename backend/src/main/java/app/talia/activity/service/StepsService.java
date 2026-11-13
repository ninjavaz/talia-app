package app.talia.activity.service;

import app.talia.common.Weeks;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/** Average daily steps of a week: the manual weekly value if entered, otherwise the average of synced days. */
@Component
public class StepsService {

    private final JdbcClient jdbc;

    StepsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Integer> avgDailySteps(long userId, LocalDate anyDayOfWeek) {
        LocalDate start = Weeks.start(anyDayOfWeek);
        return jdbc.sql("""
                        SELECT COALESCE(
                            (SELECT avg_daily_steps FROM weekly_steps WHERE user_id = :user AND week_start = :start),
                            (SELECT round(avg(steps))::int FROM daily_steps
                             WHERE user_id = :user AND day BETWEEN :start AND :end))
                        """)
                .param("user", userId)
                .param("start", start)
                .param("end", start.plusDays(6))
                .query(Integer.class)
                .optional();
    }
}
