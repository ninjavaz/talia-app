package app.talia.habit.model;

/** doneToday / total: today's progress (REQ-014); streakDays: consecutive completed days (REQ-015). */
public record HabitStats(int doneToday, int total, int streakDays) {
}
