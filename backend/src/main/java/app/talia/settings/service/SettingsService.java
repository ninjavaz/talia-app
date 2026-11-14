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
        entity.setHabitWeekThresholdPct(s.habitWeekThresholdPct());
        entity.setLanguage(s.language());
        return toModel(entity);
    }

    /** Default settings for a new account. */
    public void createDefaults(long userId, String language) {
        settings.save(new UserSettingsEntity(userId, language));
    }

    private static AppSettings toModel(UserSettingsEntity e) {
        return new AppSettings(e.getWeeklyWorkoutGoal(), e.getDailyStepsGoal(), e.getHabitWeekThresholdPct(),
                e.getLanguage());
    }
}
