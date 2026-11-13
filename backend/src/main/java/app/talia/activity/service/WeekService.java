package app.talia.activity.service;

import app.talia.activity.model.WeekView;
import app.talia.activity.model.Workout;
import app.talia.common.Weeks;
import app.talia.habit.service.HabitService;
import app.talia.settings.model.AppSettings;
import app.talia.settings.service.SettingsService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Everything for the "Week" screen: workouts against the goal, average steps and habit stats. */
@Service
public class WeekService {

    private final WorkoutService workouts;
    private final StepsService steps;
    private final HabitService habits;
    private final SettingsService settings;
    private final Clock clock;

    WeekService(WorkoutService workouts, StepsService steps, HabitService habits, SettingsService settings,
                Clock clock) {
        this.workouts = workouts;
        this.steps = steps;
        this.habits = habits;
        this.settings = settings;
        this.clock = clock;
    }

    /** The week containing {@code day} (default: the current week). */
    public WeekView week(long userId, LocalDate day) {
        LocalDate today = LocalDate.now(clock);
        LocalDate anyDay = day != null ? day : today;
        LocalDate start = Weeks.start(anyDay);
        LocalDate end = start.plusDays(6);
        List<Workout> list = workouts.between(userId, start, end);
        AppSettings s = settings.get(userId);
        // habit stats up to today for the current week, the whole week for past weeks
        LocalDate statsDay = end.isAfter(today) ? today : end;
        return new WeekView(start, end, s.weeklyWorkoutGoal(), list.size(), list,
                steps.avgDailySteps(userId, anyDay).orElse(null), s.dailyStepsGoal(),
                habits.stats(userId, statsDay));
    }
}
