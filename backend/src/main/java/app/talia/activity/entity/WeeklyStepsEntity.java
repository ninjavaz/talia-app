package app.talia.activity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;

/** Table {@code weekly_steps}: average daily steps entered in the weekly check-in (wins over synced daily steps). */
@Entity
@Table(name = "weekly_steps")
@IdClass(WeeklyStepsEntity.Key.class)
public class WeeklyStepsEntity {

    public record Key(Long userId, LocalDate weekStart) implements Serializable {
    }

    @Id
    private Long userId;

    /** Monday of the week. */
    @Id
    private LocalDate weekStart;

    private int avgDailySteps;

    protected WeeklyStepsEntity() {
    }

    public WeeklyStepsEntity(long userId, LocalDate weekStart, int avgDailySteps) {
        this.userId = userId;
        this.weekStart = weekStart;
        this.avgDailySteps = avgDailySteps;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public int getAvgDailySteps() {
        return avgDailySteps;
    }
}
