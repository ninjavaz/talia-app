package app.talia.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table {@code user_settings}: one row per account, created together with the account. */
@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private int weeklyWorkoutGoal = 3;
    private int dailyStepsGoal = 8000;
    private int habitWeekThresholdPct = 70;
    private String language = "pl";

    protected UserSettingsEntity() {
    }

    /** Default settings of a new account. */
    public UserSettingsEntity(long userId, String language) {
        this.userId = userId;
        this.language = language;
    }

    public Long getUserId() {
        return userId;
    }

    public int getWeeklyWorkoutGoal() {
        return weeklyWorkoutGoal;
    }

    public void setWeeklyWorkoutGoal(int weeklyWorkoutGoal) {
        this.weeklyWorkoutGoal = weeklyWorkoutGoal;
    }

    public int getDailyStepsGoal() {
        return dailyStepsGoal;
    }

    public void setDailyStepsGoal(int dailyStepsGoal) {
        this.dailyStepsGoal = dailyStepsGoal;
    }

    public int getHabitWeekThresholdPct() {
        return habitWeekThresholdPct;
    }

    public void setHabitWeekThresholdPct(int habitWeekThresholdPct) {
        this.habitWeekThresholdPct = habitWeekThresholdPct;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
