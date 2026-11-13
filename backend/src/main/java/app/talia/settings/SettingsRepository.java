package app.talia.settings;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The single row of app_settings. */
@Repository
public class SettingsRepository {

    private final JdbcClient jdbc;

    SettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public AppSettings get() {
        return jdbc.sql("""
                        SELECT weekly_workout_goal, daily_steps_goal, habit_week_threshold_pct, language
                        FROM app_settings WHERE id = 1
                        """)
                .query((rs, n) -> new AppSettings(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4)))
                .single();
    }

    AppSettings update(AppSettings s) {
        jdbc.sql("""
                        UPDATE app_settings
                        SET weekly_workout_goal = :workouts, daily_steps_goal = :steps,
                            habit_week_threshold_pct = :threshold, language = :language
                        WHERE id = 1
                        """)
                .param("workouts", s.weeklyWorkoutGoal())
                .param("steps", s.dailyStepsGoal())
                .param("threshold", s.habitWeekThresholdPct())
                .param("language", s.language())
                .update();
        return get();
    }
}
