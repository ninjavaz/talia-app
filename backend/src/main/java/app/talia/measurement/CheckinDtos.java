package app.talia.measurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class CheckinDtos {

    private CheckinDtos() {
    }

    record PartValue(@NotNull Long partId, @NotNull @DecimalMin("5") @DecimalMax("300") BigDecimal valueCm) {
    }

    /**
     * Weekly check-in. It replaces the manual data of that week: a body part, weight or steps that is missing here
     * is removed from the week (the form always sends the complete, prefilled state).
     */
    record CheckinRequest(
            @NotNull LocalDate date,
            @Size(max = 30) List<@Valid @NotNull PartValue> measurements,
            @DecimalMin("20") @DecimalMax("400") BigDecimal weightKg,
            @PositiveOrZero @Max(100_000) Integer avgDailySteps) {

        CheckinRequest {
            measurements = measurements == null ? List.of() : measurements;
        }

        @AssertTrue(message = "at least one value is required")
        boolean isNotEmpty() {
            return !measurements.isEmpty() || weightKg != null || avgDailySteps != null;
        }
    }

    /** One week of history. Weight is the average of that week's entries (manual and synced). */
    record WeekEntry(LocalDate weekStart, List<PartValue> measurements, BigDecimal weightKg, Integer avgDailySteps) {
    }

    record Point(LocalDate date, BigDecimal value) {
    }

    /** latest/first are the newest and oldest values; delta = latest - first (negative = less). */
    record Metric(BigDecimal latest, LocalDate latestDate, BigDecimal first, BigDecimal delta) {
    }

    record BodyPartProgress(long id, String name, String code, Metric metric, List<Point> points) {
    }

    record ProgressView(
            Metric waist,
            Metric weight,
            List<BodyPartProgress> bodyParts,
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
