package app.talia.habit.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;

/** Table {@code habit_check}: the habit was done on that day. */
@Entity
@Table(name = "habit_check")
@IdClass(HabitCheckEntity.Key.class)
public class HabitCheckEntity {

    public record Key(Long habitId, LocalDate day) implements Serializable {
    }

    @Id
    private Long habitId;

    @Id
    private LocalDate day;

    protected HabitCheckEntity() {
    }

    public Long getHabitId() {
        return habitId;
    }

    public LocalDate getDay() {
        return day;
    }
}
