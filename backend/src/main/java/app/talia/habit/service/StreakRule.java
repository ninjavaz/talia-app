package app.talia.habit.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

/**
 * REQ-015, the one place that defines the habit streak:
 * <ul>
 *   <li>a day is completed when every habit active on that day is done (a day without active habits is not);</li>
 *   <li>the streak is the number of consecutive completed days ending today if today is completed, otherwise
 *       ending yesterday - an incomplete today never breaks the streak.</li>
 * </ul>
 */
public final class StreakRule {

    /** Upper bound of the streak (and of the days looked at). */
    public static final int MAX_DAYS = 3650;

    private StreakRule() {
    }

    public static boolean isCompleted(Set<Long> activeHabitIds, Set<Long> doneHabitIds) {
        return !activeHabitIds.isEmpty() && doneHabitIds.containsAll(activeHabitIds);
    }

    public static int streak(LocalDate today, Predicate<LocalDate> completed) {
        LocalDate day = completed.test(today) ? today : today.minusDays(1);
        int streak = 0;
        while (streak < MAX_DAYS && completed.test(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
