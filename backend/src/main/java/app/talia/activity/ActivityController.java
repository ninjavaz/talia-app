package app.talia.activity;

import app.talia.activity.WorkoutService.Workout;
import app.talia.common.Weeks;
import app.talia.habit.HabitService;
import app.talia.habit.HabitService.HabitStats;
import app.talia.settings.AppSettings;
import app.talia.settings.SettingsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
class ActivityController {

    record WorkoutRequest(@NotNull LocalDate date, @NotNull WorkoutCategory category,
                          @NotNull @Min(1) @Max(600) Integer durationMin) {
    }

    record WeekView(LocalDate weekStart, LocalDate weekEnd, int workoutGoal, int workoutsDone,
                    List<Workout> workouts, Integer avgDailySteps, int dailyStepsGoal, HabitStats habits) {
    }

    private final WorkoutService workouts;
    private final StepsQueries steps;
    private final HabitService habits;
    private final SettingsRepository settings;
    private final Clock clock;

    ActivityController(WorkoutService workouts, StepsQueries steps, HabitService habits,
                       SettingsRepository settings, Clock clock) {
        this.workouts = workouts;
        this.steps = steps;
        this.habits = habits;
        this.settings = settings;
        this.clock = clock;
    }

    /** Everything for the "Week" screen. {@code date} picks the week (default: current week). */
    @GetMapping("/api/week")
    WeekView week(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        LocalDate day = date != null ? date : today;
        LocalDate start = Weeks.start(day);
        LocalDate end = start.plusDays(6);
        List<Workout> list = workouts.between(start, end);
        AppSettings s = settings.get();
        // habit stats up to today for the current week, the whole week for past weeks
        LocalDate statsDay = end.isAfter(today) ? today : end;
        return new WeekView(start, end, s.weeklyWorkoutGoal(), list.size(), list,
                steps.avgDailySteps(day).orElse(null), s.dailyStepsGoal(), habits.stats(statsDay));
    }

    @GetMapping("/api/workouts")
    List<Workout> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return workouts.between(from, to);
    }

    @PostMapping("/api/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    Workout add(@Valid @RequestBody WorkoutRequest body) {
        return workouts.add(body.date(), body.category(), body.durationMin());
    }

    @DeleteMapping("/api/workouts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        workouts.delete(id);
    }
}
