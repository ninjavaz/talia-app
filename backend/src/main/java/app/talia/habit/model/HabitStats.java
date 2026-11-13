package app.talia.habit.model;

/** weekPct: share of possible checks done this week so far; streakWeeks: weeks in a row meeting the threshold. */
public record HabitStats(int doneToday, int total, int weekPct, int thresholdPct, int streakWeeks) {
}
