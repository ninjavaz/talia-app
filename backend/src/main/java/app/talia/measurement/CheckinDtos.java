package app.talia.measurement;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class CheckinDtos {

    private CheckinDtos() {
    }

    /** Weekly check-in. Each value is optional; a missing value leaves that metric of the week untouched. */
    record CheckinRequest(
            @NotNull LocalDate date,
            @DecimalMin("30") @DecimalMax("200") BigDecimal waistCm,
            @DecimalMin("20") @DecimalMax("400") BigDecimal weightKg,
            @PositiveOrZero @Max(100_000) Integer avgDailySteps) {

        @AssertTrue(message = "at least one value is required")
        boolean isNotEmpty() {
            return waistCm != null || weightKg != null || avgDailySteps != null;
        }
    }

    /** One week of history. Weight is the average of that week's entries (manual and synced). */
    record WeekEntry(LocalDate weekStart, BigDecimal waistCm, BigDecimal weightKg, Integer avgDailySteps) {
    }

    record Point(LocalDate date, BigDecimal value) {
    }

    /** latest/first are the newest and oldest values; delta = latest - first (negative = less). */
    record Metric(BigDecimal latest, LocalDate latestDate, BigDecimal first, BigDecimal delta) {
    }

    record ProgressView(
            Metric waist,
            Metric weight,
            List<Point> waistPoints,
            List<Point> weightPoints,
            List<Point> weightTrend,
            Integer avgDailyStepsThisWeek,
            Integer avgDailyStepsLastWeek,
            int dailyStepsGoal,
            LocalDate lastCheckin,
            boolean checkinDoneThisWeek,
            boolean checkinDue) {
    }
}
