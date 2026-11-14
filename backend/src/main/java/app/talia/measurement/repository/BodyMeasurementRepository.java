package app.talia.measurement.repository;

import app.talia.measurement.entity.BodyMeasurementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurementEntity, Long> {

    /** Latest value of a body part in one week. */
    interface WeekValue {
        LocalDate getWeekStart();

        Long getPartId();

        BigDecimal getValueCm();

        Integer getSortOrder();
    }

    List<BodyMeasurementEntity> findByBodyPartIdOrderByMeasuredOn(long bodyPartId);

    /** Latest value per (week, body part) of the user's active body parts. */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT ON (date_trunc('week', m.measured_on), m.body_part_id)
                   date_trunc('week', m.measured_on)::date AS week_start, m.body_part_id AS part_id,
                   m.value_cm AS value_cm, p.sort_order AS sort_order
            FROM body_measurement m JOIN body_part p ON p.id = m.body_part_id AND p.active AND p.user_id = :userId
            ORDER BY date_trunc('week', m.measured_on), m.body_part_id, m.measured_on DESC
            """)
    List<WeekValue> findLatestPerWeek(long userId);

    @Query("""
            select max(m.measuredOn) from BodyMeasurementEntity m, BodyPartEntity p
            where p.id = m.bodyPartId and p.userId = :userId and p.active = true
            """)
    Optional<LocalDate> findLastDate(long userId);

    /** Measurements of the user's active body parts in [start, end]. */
    @Modifying
    @Query("""
            delete from BodyMeasurementEntity m
            where m.measuredOn between :start and :end
              and m.bodyPartId in (select p.id from BodyPartEntity p where p.userId = :userId and p.active = true)
            """)
    void deleteBetween(long userId, LocalDate start, LocalDate end);
}
