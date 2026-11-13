package app.talia.activity.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Workout(long id, LocalDateTime startedAt, int durationMin, String type, WorkoutCategory category,
                      BigDecimal kcal) {
}
