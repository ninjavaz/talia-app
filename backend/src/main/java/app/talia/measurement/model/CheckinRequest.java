package app.talia.measurement.model;

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

/**
 * Weekly check-in. It replaces the manual data of that week: a body part, weight or steps that is missing here
 * is removed from the week (the form always sends the complete, prefilled state).
 */
public record CheckinRequest(
        @NotNull LocalDate date,
        @Size(max = 30) List<@Valid @NotNull PartValue> measurements,
        @DecimalMin("20") @DecimalMax("400") BigDecimal weightKg,
        @PositiveOrZero @Max(100_000) Integer avgDailySteps) {

    public CheckinRequest {
        measurements = measurements == null ? List.of() : measurements;
    }

    @AssertTrue(message = "at least one value is required")
    public boolean isNotEmpty() {
        return !measurements.isEmpty() || weightKg != null || avgDailySteps != null;
    }
}
