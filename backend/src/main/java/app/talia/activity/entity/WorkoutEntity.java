package app.talia.activity.entity;

import app.talia.activity.model.WorkoutCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Table {@code workout}: manual workouts and the ones synced from Apple Health (unique per user + start + type). */
@Entity
@Table(name = "workout")
public class WorkoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String type;

    @Enumerated(EnumType.STRING)
    private WorkoutCategory category;

    private BigDecimal kcal;

    protected WorkoutEntity() {
    }

    public WorkoutEntity(long userId, LocalDateTime startedAt, LocalDateTime endedAt, String type,
                         WorkoutCategory category) {
        this.userId = userId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.type = type;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public String getType() {
        return type;
    }

    public WorkoutCategory getCategory() {
        return category;
    }

    public BigDecimal getKcal() {
        return kcal;
    }
}
