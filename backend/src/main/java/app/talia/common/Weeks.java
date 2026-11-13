package app.talia.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Weeks are ISO weeks: Monday to Sunday (a Sunday check-in belongs to the week that ends that day). */
public final class Weeks {

    private Weeks() {
    }

    public static LocalDate start(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate end(LocalDate date) {
        return start(date).plusDays(6);
    }
}
