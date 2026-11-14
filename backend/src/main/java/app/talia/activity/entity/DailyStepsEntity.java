package app.talia.activity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;

/** Table {@code daily_steps}: steps per day synced from Apple Health (written by an upsert, see the repository). */
@Entity
@Table(name = "daily_steps")
@IdClass(DailyStepsEntity.Key.class)
public class DailyStepsEntity {

    public record Key(Long userId, LocalDate day) implements Serializable {
    }

    @Id
    private Long userId;

    @Id
    private LocalDate day;

    private int steps;

    protected DailyStepsEntity() {
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getDay() {
        return day;
    }

    public int getSteps() {
        return steps;
    }
}
