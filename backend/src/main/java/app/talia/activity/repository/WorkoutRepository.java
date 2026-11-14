package app.talia.activity.repository;

import app.talia.activity.entity.WorkoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<WorkoutEntity, Long> {

    /** Workouts starting in [from, to). */
    List<WorkoutEntity> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAt(
            long userId, LocalDateTime from, LocalDateTime to);

    boolean existsByUserIdAndStartedAtAndType(long userId, LocalDateTime startedAt, String type);

    long deleteByIdAndUserId(long id, long userId);

    /**
     * Idempotent upsert from Apple Health. The row is only touched when a value changed; returns true = inserted,
     * false = updated, empty = already there unchanged ({@code xmax = 0} is true for a freshly inserted row).
     */
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO workout (user_id, started_at, ended_at, type, category, kcal)
            VALUES (:userId, :start, :end, :type, :category, :kcal)
            ON CONFLICT (user_id, started_at, type) DO UPDATE
               SET ended_at = EXCLUDED.ended_at, category = EXCLUDED.category, kcal = EXCLUDED.kcal, updated_at = now()
            WHERE (workout.ended_at, workout.category, workout.kcal)
                  IS DISTINCT FROM (EXCLUDED.ended_at, EXCLUDED.category, EXCLUDED.kcal)
            RETURNING (xmax = 0)
            """)
    Optional<Boolean> upsertFromHealth(long userId, LocalDateTime start, LocalDateTime end, String type,
                                       String category, BigDecimal kcal);
}
