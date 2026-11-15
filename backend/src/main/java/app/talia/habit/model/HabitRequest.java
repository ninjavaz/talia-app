package app.talia.habit.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Create or update a habit. {@code name} is required for NORMAL habits and must be absent for the STEPS habit;
 * {@code remindAt} is optional ("HH:mm", null = no time); {@code kind} is only read on create (default NORMAL).
 */
public record HabitRequest(@Size(max = 100) String name, @JsonFormat(pattern = "HH:mm") LocalTime remindAt,
                           HabitKind kind) {
}
