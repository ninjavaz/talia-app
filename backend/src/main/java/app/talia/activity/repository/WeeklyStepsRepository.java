package app.talia.activity.repository;

import app.talia.activity.entity.WeeklyStepsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyStepsRepository extends JpaRepository<WeeklyStepsEntity, WeeklyStepsEntity.Key> {

    List<WeeklyStepsEntity> findByUserId(long userId);

    @Query("select max(w.weekStart) from WeeklyStepsEntity w where w.userId = :userId")
    Optional<LocalDate> findLastWeekStart(long userId);

    @Modifying
    @Query("delete from WeeklyStepsEntity w where w.userId = :userId and w.weekStart = :weekStart")
    void deleteWeek(long userId, LocalDate weekStart);

    /** The manual weekly value if entered, otherwise the average of the synced days of that week. */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE(
                (SELECT avg_daily_steps FROM weekly_steps WHERE user_id = :userId AND week_start = :start),
                (SELECT round(avg(steps))::int FROM daily_steps
                 WHERE user_id = :userId AND day BETWEEN :start AND :end))
            """)
    Optional<Integer> avgDailySteps(long userId, LocalDate start, LocalDate end);
}
