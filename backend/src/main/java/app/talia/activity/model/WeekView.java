package app.talia.activity.model;

import app.talia.habit.model.HabitStats;

import java.time.LocalDate;
import java.util.List;

/** Everything for the Week screen; {@code kcalTotal}: sum of the week's workout kcal, null when none has kcal. */
public record WeekView(LocalDate weekStart, LocalDate weekEnd, int workoutGoal, int workoutsDone,
                List<Workout> workouts, Integer kcalTotal, Integer avgDailySteps, int dailyStepsGoal,
                HabitStats habits) {
}
