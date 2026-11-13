package app.talia.sync.model;

import app.talia.activity.model.Workout;
import app.talia.sync.service.FlexibleDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload sent by the iOS Shortcut. Timestamps are strings on purpose: Shortcuts formats dates in several ways
 * (with or without offset, with a space instead of 'T'), {@link FlexibleDateTime} normalises them.
 */
public record HealthSyncRequest(
        @Size(max = 400) List<@Valid @NotNull Steps> steps,
        @Size(max = 400) List<@Valid @NotNull Weight> weight,
        @Size(max = 400) List<@Valid @NotNull Workout> workouts) {

    public HealthSyncRequest {
        steps = steps == null ? List.of() : steps;
        weight = weight == null ? List.of() : weight;
        workouts = workouts == null ? List.of() : workouts;
    }

    public record Steps(@NotNull LocalDate date, @NotNull @PositiveOrZero @Max(200_000) Integer value) {
    }

    public record Weight(@NotBlank String ts, @NotNull @DecimalMin("20") @DecimalMax("400") BigDecimal value) {
    }

    public record Workout(@NotBlank String start, @NotBlank String end, @NotBlank @Size(max = 100) String type,
                   @PositiveOrZero BigDecimal kcal) {
    }
}
