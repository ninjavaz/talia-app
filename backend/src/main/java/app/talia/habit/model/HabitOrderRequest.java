package app.talia.habit.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The user's active habit ids in the new order (REQ-010); must contain each of them exactly once. */
public record HabitOrderRequest(@NotNull List<@NotNull Long> ids) {
}
