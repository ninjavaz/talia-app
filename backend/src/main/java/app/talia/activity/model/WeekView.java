package app.talia.activity.model;

import app.talia.habit.model.HabitStats;

import java.time.LocalDate;
import java.util.List;

public record WeekView(LocalDate weekStart, LocalDate weekEnd, int workoutGoal, int workoutsDone,
                List<Workout> workouts, Integer avgDailySteps, int dailyStepsGoal, HabitStats habits) {
}
