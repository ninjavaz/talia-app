package app.talia.habit.entity;

import app.talia.habit.model.HabitKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

/** Table {@code habit}: a daily habit of the user. Deleting only deactivates it, so past checks stay. */
@Entity
@Table(name = "habit")
public class HabitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;
    private String name;

    @Enumerated(EnumType.STRING)
    private HabitKind kind = HabitKind.NORMAL;

    /** Optional time of day (REQ-011). */
    private LocalTime remindAt;

    private int sortOrder;
    private boolean active = true;

    /** First day the habit counts for the streak (REQ-015). */
    private LocalDate createdOn;

    /** Day it was deleted (from this day on it no longer counts); null while active. */
    private LocalDate deactivatedOn;

    protected HabitEntity() {
    }

    public HabitEntity(long userId, String name, HabitKind kind, LocalTime remindAt, int sortOrder, LocalDate createdOn) {
        this.userId = userId;
        this.name = name;
        this.kind = kind;
        this.remindAt = remindAt;
        this.sortOrder = sortOrder;
        this.createdOn = createdOn;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HabitKind getKind() {
        return kind;
    }

    public LocalTime getRemindAt() {
        return remindAt;
    }

    public void setRemindAt(LocalTime remindAt) {
        this.remindAt = remindAt;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public LocalDate getDeactivatedOn() {
        return deactivatedOn;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    /** Deletes the habit from {@code today} on; its past checks stay. */
    public void deactivate(LocalDate today) {
        this.active = false;
        this.deactivatedOn = today.isBefore(createdOn) ? createdOn : today;
    }

    /** Whether the habit counts on {@code day} (added on or before it and not deleted on or before it). */
    public boolean isActiveOn(LocalDate day) {
        return !day.isBefore(createdOn) && (deactivatedOn == null || day.isBefore(deactivatedOn));
    }
}
