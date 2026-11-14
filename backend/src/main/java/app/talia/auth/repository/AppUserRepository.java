package app.talia.auth.repository;

import app.talia.auth.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByUsername(String username);

    Optional<AppUserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select u.id from AppUserEntity u where u.syncTokenHash = :hash")
    Optional<Long> findIdBySyncTokenHash(String hash);

    /** Accounts whose e-mail was never confirmed (their data goes with them: ON DELETE CASCADE). */
    @Modifying
    @Query("delete from AppUserEntity u where u.emailVerifiedAt is null and u.createdAt < :cutoff")
    int deleteUnverifiedCreatedBefore(Instant cutoff);
}
