package app.talia.push.repository;

import app.talia.push.entity.PushSentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface PushSentRepository extends JpaRepository<PushSentEntity, PushSentEntity.Key> {

    /** Claims a reminder: 1 if it was not sent before (atomic, so parallel runs cannot both send it), else 0. */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO push_sent (user_id, kind, ref_id, day) VALUES (:userId, :kind, :refId, :day)
            ON CONFLICT DO NOTHING
            """)
    int claim(long userId, String kind, long refId, LocalDate day);

    @Modifying
    @Transactional
    @Query("delete from PushSentEntity s where s.day < :before")
    int deleteOlderThan(LocalDate before);
}
