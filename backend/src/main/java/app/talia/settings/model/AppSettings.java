package app.talia.settings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

/**
 * Settings of one user. The reminder fields (REQ-033) are optional on update: null keeps the current value.
 * {@code checkinReminderDay}: ISO day of week (1 = Monday ... 7 = Sunday); times as "HH:mm".
 */
public record AppSettings(
        @NotNull @Min(0) @Max(21) Integer weeklyWorkoutGoal,
        @NotNull @Min(0) @Max(100_000) Integer dailyStepsGoal,
        @NotNull @Pattern(regexp = "pl|en") String language,
        Boolean pushHabits,
        Boolean pushCheckin,
        @Min(1) @Max(7) Integer checkinReminderDay,
        @JsonFormat(pattern = "HH:mm") LocalTime checkinReminderTime,
        Boolean pushStreak,
        @JsonFormat(pattern = "HH:mm") LocalTime streakReminderTime) {
}
