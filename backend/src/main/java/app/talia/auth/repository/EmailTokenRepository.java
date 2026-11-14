package app.talia.auth.repository;

import app.talia.auth.entity.EmailTokenEntity;
import app.talia.auth.model.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailTokenEntity, String> {

    /** User id of an unexpired token. */
    @Query("""
            select t.userId from EmailTokenEntity t
            where t.tokenHash = :hash and t.purpose = :purpose and t.expiresAt > :now
            """)
    Optional<Long> findValidUserId(String hash, TokenPurpose purpose, Instant now);

    @Modifying
    @Query("delete from EmailTokenEntity t where t.userId = :userId and t.purpose = :purpose")
    void deleteByUser(long userId, TokenPurpose purpose);

    @Modifying
    @Query("delete from EmailTokenEntity t where t.userId = :userId")
    void deleteAllByUser(long userId);

    @Modifying
    @Query("delete from EmailTokenEntity t where t.expiresAt < :now")
    int deleteExpired(Instant now);
}
