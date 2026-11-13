package app.talia.activity.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WorkoutRequest(@NotNull LocalDate date, @NotNull WorkoutCategory category,
                      @NotNull @Min(1) @Max(600) Integer durationMin) {
}
