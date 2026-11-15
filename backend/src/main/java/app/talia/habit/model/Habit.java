package app.talia.habit.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/** A habit; {@code name} is meaningless for the STEPS kind (the UI shows the steps goal instead). */
public record Habit(long id, String name, HabitKind kind, @JsonFormat(pattern = "HH:mm") LocalTime remindAt,
                    int sortOrder) {
}
