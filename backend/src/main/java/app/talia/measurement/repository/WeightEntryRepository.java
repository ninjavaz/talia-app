package app.talia.measurement.repository;

import app.talia.measurement.entity.WeightEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WeightEntryRepository extends JpaRepository<WeightEntryEntity, Long> {

    /** An average weight for a date (a day or the Monday of a week). */
    interface DatedValue {
        LocalDate getDate();

        BigDecimal getValue();
    }

    @Query(nativeQuery = true, value = """
            SELECT measured_at::date AS date, round(avg(value_kg), 2) AS value
            FROM weight_entry WHERE user_id = :userId GROUP BY 1 ORDER BY 1
            """)
    List<DatedValue> findDailyAverages(long userId);

    @Query(nativeQuery = true, value = """
            SELECT date_trunc('week', measured_at)::date AS date, round(avg(value_kg), 1) AS value
            FROM weight_entry WHERE user_id = :userId GROUP BY 1
            """)
    List<DatedValue> findWeeklyAverages(long userId);

    @Query("select max(w.measuredAt) from WeightEntryEntity w where w.userId = :userId and w.source = 'MANUAL'")
    Optional<LocalDateTime> findLastManual(long userId);

    /** Manual weight from the check-in; replaces any entry at the same timestamp. */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO weight_entry (user_id, measured_at, value_kg, source)
            VALUES (:userId, :measuredAt, :valueKg, 'MANUAL')
            ON CONFLICT (user_id, measured_at) DO UPDATE
               SET value_kg = EXCLUDED.value_kg, source = 'MANUAL', updated_at = now()
            """)
    void upsertManual(long userId, LocalDateTime measuredAt, BigDecimal valueKg);

    /** Manual entries in [from, to); synced ones stay. */
    @Modifying
    @Query("""
            delete from WeightEntryEntity w
            where w.userId = :userId and w.source = 'MANUAL' and w.measuredAt >= :from and w.measuredAt < :to
            """)
    void deleteManualBetween(long userId, LocalDateTime from, LocalDateTime to);

    /** Idempotent upsert from Apple Health: true = inserted, false = updated, empty = unchanged. */
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO weight_entry (user_id, measured_at, value_kg, source) VALUES (:userId, :measuredAt, :valueKg, 'HEALTH')
            ON CONFLICT (user_id, measured_at) DO UPDATE SET value_kg = EXCLUDED.value_kg, updated_at = now()
            WHERE weight_entry.value_kg IS DISTINCT FROM EXCLUDED.value_kg
            RETURNING (xmax = 0)
            """)
    Optional<Boolean> upsertFromHealth(long userId, LocalDateTime measuredAt, BigDecimal valueKg);
}
