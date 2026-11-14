package app.talia.activity.repository;

import app.talia.activity.entity.DailyStepsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyStepsRepository extends JpaRepository<DailyStepsEntity, DailyStepsEntity.Key> {

    /** Idempotent upsert from Apple Health: true = inserted, false = updated, empty = unchanged. */
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO daily_steps (user_id, day, steps) VALUES (:userId, :day, :steps)
            ON CONFLICT (user_id, day) DO UPDATE SET steps = EXCLUDED.steps, updated_at = now()
            WHERE daily_steps.steps IS DISTINCT FROM EXCLUDED.steps
            RETURNING (xmax = 0)
            """)
    Optional<Boolean> upsertFromHealth(long userId, LocalDate day, int steps);
}
