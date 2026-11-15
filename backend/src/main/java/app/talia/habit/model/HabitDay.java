package app.talia.habit.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/** A habit on the daily list with its done flag for that day. */
public record HabitDay(long id, String name, HabitKind kind, @JsonFormat(pattern = "HH:mm") LocalTime remindAt,
                       boolean done) {
}
