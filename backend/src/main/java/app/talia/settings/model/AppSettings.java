package app.talia.settings.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AppSettings(
        @NotNull @Min(0) @Max(21) Integer weeklyWorkoutGoal,
        @NotNull @Min(0) @Max(100_000) Integer dailyStepsGoal,
        @NotNull @Pattern(regexp = "pl|en") String language) {
}
