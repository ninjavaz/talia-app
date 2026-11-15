package app.talia.habit.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

/** REQ-015: the one place that defines when a day counts and how long the streak is. */
public final class StreakRule {

    /** Upper bound of the streak (and of the days looked at). */
    public static final int MAX_DAYS = 3650;

    private StreakRule() {
    }

    public static boolean isCompleted(Set<Long> activeHabitIds, Set<Long> doneHabitIds) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public static int streak(LocalDate today, Predicate<LocalDate> completed) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
