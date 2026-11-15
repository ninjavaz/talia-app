package app.talia.settings.service;

import app.talia.settings.entity.UserSettingsEntity;
import app.talia.settings.model.AppSettings;
import app.talia.settings.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-user settings (one row per account, created together with the account). */
@Service
@Transactional
public class SettingsService {

    private final UserSettingsRepository settings;

    SettingsService(UserSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public AppSettings get(long userId) {
        return toModel(settings.findById(userId).orElseThrow());
    }

    public AppSettings update(long userId, AppSettings s) {
        UserSettingsEntity entity = settings.findById(userId).orElseThrow();
        entity.setWeeklyWorkoutGoal(s.weeklyWorkoutGoal());
        entity.setDailyStepsGoal(s.dailyStepsGoal());
        entity.setLanguage(s.language());
        // reminder settings are optional on update (null keeps the current value)
        if (s.pushHabits() != null) {
            entity.setPushHabits(s.pushHabits());
        }
        if (s.pushCheckin() != null) {
            entity.setPushCheckin(s.pushCheckin());
        }
        if (s.pushStreak() != null) {
            entity.setPushStreak(s.pushStreak());
        }
        if (s.checkinReminderDay() != null) {
            entity.setCheckinReminderDay(s.checkinReminderDay());
        }
        if (s.checkinReminderTime() != null) {
            entity.setCheckinReminderTime(s.checkinReminderTime());
        }
        if (s.streakReminderTime() != null) {
            entity.setStreakReminderTime(s.streakReminderTime());
        }
        return toModel(entity);
    }

    /** Default settings for a new account. */
    public void createDefaults(long userId, String language) {
        settings.save(new UserSettingsEntity(userId, language));
    }

    private static AppSettings toModel(UserSettingsEntity e) {
        return new AppSettings(e.getWeeklyWorkoutGoal(), e.getDailyStepsGoal(), e.getLanguage(), e.isPushHabits(),
                e.isPushCheckin(), e.getCheckinReminderDay(), e.getCheckinReminderTime(), e.isPushStreak(),
                e.getStreakReminderTime());
    }
}
