package app.talia.habit.repository;

import app.talia.habit.entity.HabitCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HabitCheckRepository extends JpaRepository<HabitCheckEntity, HabitCheckEntity.Key> {

    /** Checks per week (Monday), counting active habits only. */
    interface WeekCount {
        LocalDate getWeekStart();

        Long getChecks();
    }

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

    @Query(nativeQuery = true, value = """
            SELECT date_trunc('week', c.day)::date AS week_start, count(*) AS checks
            FROM habit_check c JOIN habit h ON h.id = c.habit_id AND h.active AND h.user_id = :userId
            WHERE c.day <= :until GROUP BY 1
            """)
    List<WeekCount> countPerWeek(long userId, LocalDate until);
}
