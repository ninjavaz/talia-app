package app.talia.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AppSettings(
        @NotNull @Min(0) @Max(21) Integer weeklyWorkoutGoal,
        @NotNull @Min(0) @Max(100_000) Integer dailyStepsGoal,
        @NotNull @Min(1) @Max(100) Integer habitWeekThresholdPct,
        @NotNull @Pattern(regexp = "pl|en") String language) {
}
