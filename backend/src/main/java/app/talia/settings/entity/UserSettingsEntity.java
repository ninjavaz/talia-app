package app.talia.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

/** Table {@code user_settings}: one row per account, created together with the account. */
@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private int weeklyWorkoutGoal = 3;
    private int dailyStepsGoal = 8000;
    private String language = "pl";
    private boolean pushHabits = true;
    private boolean pushCheckin = true;
    private boolean pushStreak = true;
    private int checkinReminderDay = 7;
    private LocalTime checkinReminderTime = LocalTime.of(10, 0);
    private LocalTime streakReminderTime = LocalTime.of(20, 0);

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isPushHabits() {
        return pushHabits;
    }

    public void setPushHabits(boolean pushHabits) {
        this.pushHabits = pushHabits;
    }

    public boolean isPushCheckin() {
        return pushCheckin;
    }

    public void setPushCheckin(boolean pushCheckin) {
        this.pushCheckin = pushCheckin;
    }

    public boolean isPushStreak() {
        return pushStreak;
    }

    public void setPushStreak(boolean pushStreak) {
        this.pushStreak = pushStreak;
    }

    public int getCheckinReminderDay() {
        return checkinReminderDay;
    }

    public void setCheckinReminderDay(int checkinReminderDay) {
        this.checkinReminderDay = checkinReminderDay;
    }

    public LocalTime getCheckinReminderTime() {
        return checkinReminderTime;
    }

    public void setCheckinReminderTime(LocalTime checkinReminderTime) {
        this.checkinReminderTime = checkinReminderTime;
    }

    public LocalTime getStreakReminderTime() {
        return streakReminderTime;
    }

    public void setStreakReminderTime(LocalTime streakReminderTime) {
        this.streakReminderTime = streakReminderTime;
    }
}
