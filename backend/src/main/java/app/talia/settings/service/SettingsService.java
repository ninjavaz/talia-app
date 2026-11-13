package app.talia.settings.service;

import app.talia.settings.model.AppSettings;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Per-user settings (one row per account, created together with the account). */
@Repository
public class SettingsService {

    private final JdbcClient jdbc;

    SettingsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public AppSettings get(long userId) {
        return jdbc.sql("""
                        SELECT weekly_workout_goal, daily_steps_goal, habit_week_threshold_pct, language
                        FROM user_settings WHERE user_id = :user
                        """)
                .param("user", userId)
                .query((rs, n) -> new AppSettings(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4)))
                .single();
    }

    public AppSettings update(long userId, AppSettings s) {
        jdbc.sql("""
                        UPDATE user_settings
                        SET weekly_workout_goal = :workouts, daily_steps_goal = :steps,
                            habit_week_threshold_pct = :threshold, language = :language
                        WHERE user_id = :user
                        """)
                .param("workouts", s.weeklyWorkoutGoal())
                .param("steps", s.dailyStepsGoal())
                .param("threshold", s.habitWeekThresholdPct())
                .param("language", s.language())
                .param("user", userId)
                .update();
        return get(userId);
    }
}
