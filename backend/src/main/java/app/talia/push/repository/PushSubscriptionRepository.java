package app.talia.push.repository;

import app.talia.push.entity.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    List<PushSubscriptionEntity> findByUserIdOrderByCreatedAtAscIdAsc(long userId);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    @Modifying
    @Transactional
    @Query("delete from PushSubscriptionEntity s where s.userId = :userId and s.endpoint = :endpoint")
    int deleteByUserAndEndpoint(long userId, String endpoint);

    @Modifying
    @Transactional
    @Query("update PushSubscriptionEntity s set s.lastUsedAt = :now where s.id = :id")
    void markUsed(long id, Instant now);

    /** Users that can receive push notifications (the reminder scheduler only looks at them). */
    @Query("select distinct s.userId from PushSubscriptionEntity s")
    List<Long> findUserIds();
}
