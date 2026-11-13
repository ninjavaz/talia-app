package app.talia.habit.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabitRequest(@NotBlank @Size(max = 100) String name) {
}
