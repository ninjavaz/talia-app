package app.talia.activity.model;

import java.time.LocalDateTime;

/**
 * A workout. {@code kcal}: burned kcal, from Apple Health when synced (> 0), otherwise estimated (REQ-022,
 * {@code kcalEstimated = true}); null when there is no weight to estimate with.
 */
public record Workout(long id, LocalDateTime startedAt, int durationMin, String type, WorkoutCategory category,
                      Integer kcal, boolean kcalEstimated) {
}
