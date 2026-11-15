package app.talia.habit.repository;

import app.talia.habit.entity.HabitCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HabitCheckRepository extends JpaRepository<HabitCheckEntity, HabitCheckEntity.Key> {

    /** Ids of the user's active habits that are done on the day. */
    @Query("""
            select c.habitId from HabitCheckEntity c, HabitEntity h
            where h.id = c.habitId and h.userId = :userId and h.active = true and c.day = :day
            """)
    List<Long> findDoneHabitIds(long userId, LocalDate day);

    /** Idempotent: checking an already checked habit changes nothing. */
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO habit_check (habit_id, day) VALUES (:habitId, :day) ON CONFLICT DO NOTHING")
    void check(long habitId, LocalDate day);

    @Modifying
    @Query("delete from HabitCheckEntity c where c.habitId = :habitId and c.day = :day")
    void uncheck(long habitId, LocalDate day);

    /** All checks of the user's habits (active and deleted) between the two days, inclusive. */
    @Query("""
            select c from HabitCheckEntity c, HabitEntity h
            where h.id = c.habitId and h.userId = :userId and c.day between :from and :to
            """)
    List<HabitCheckEntity> findChecks(long userId, LocalDate from, LocalDate to);
}
