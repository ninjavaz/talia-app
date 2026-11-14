package app.talia.auth.repository;

import app.talia.auth.entity.AltchaUsedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface AltchaUsedRepository extends JpaRepository<AltchaUsedEntity, String> {

    /** Marks a solution as used; 1 if it was new, 0 if it was used before (atomic, safe against parallel replays). */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO altcha_used (signature, expires_at) VALUES (:signature, :expiresAt)
            ON CONFLICT (signature) DO NOTHING
            """)
    int markUsed(String signature, Instant expiresAt);

    @Modifying
    @Query("delete from AltchaUsedEntity a where a.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
