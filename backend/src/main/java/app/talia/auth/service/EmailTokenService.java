package app.talia.auth.service;

import app.talia.auth.model.TokenPurpose;
import app.talia.common.Hashes;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * One-time tokens of the e-mail links (confirm address, reset password). Only their SHA-256 is stored;
 * a new token replaces the earlier ones of the same purpose.
 */
@Service
public class EmailTokenService {

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountService accounts;
    private final Clock clock;

    EmailTokenService(JdbcClient jdbc, PasswordEncoder passwordEncoder, UserAccountService accounts, Clock clock) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.accounts = accounts;
        this.clock = clock;
    }

    /** A new one-time token for an e-mail link; earlier tokens of the same purpose stop working. */
    @Transactional
    public String newToken(long userId, TokenPurpose purpose, Duration ttl) {
        jdbc.sql("DELETE FROM email_token WHERE user_id = :id AND purpose = :purpose")
                .param("id", userId).param("purpose", purpose.name()).update();
        String token = Hashes.randomToken();
        Instant now = clock.instant();
        jdbc.sql("""
                        INSERT INTO email_token (token_hash, user_id, purpose, expires_at, created_at)
                        VALUES (:hash, :id, :purpose, :expires, :now)
                        """)
                .param("hash", Hashes.sha256(token)).param("id", userId).param("purpose", purpose.name())
                .param("expires", Timestamp.from(now.plus(ttl))).param("now", Timestamp.from(now))
                .update();
        return token;
    }

    /** Confirms the e-mail address of the token's account; false if the link is invalid or expired. */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<Long> userId = userFor(token, TokenPurpose.VERIFY);
        userId.ifPresent(id -> {
            jdbc.sql("DELETE FROM email_token WHERE user_id = :id AND purpose = 'VERIFY'").param("id", id).update();
            jdbc.sql("UPDATE app_user SET email_verified_at = coalesce(email_verified_at, :now) WHERE id = :id")
                    .param("now", now()).param("id", id).update();
        });
        return userId.isPresent();
    }

    /**
     * Sets a new password from a reset link. Also confirms the e-mail (the link proves access to it) and ends all
     * sessions of the account on every device. Returns the username, or empty if the link is invalid or expired.
     */
    @Transactional
    public Optional<String> resetPassword(String token, String newPassword) {
        Optional<Long> userId = userFor(token, TokenPurpose.RESET);
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        long id = userId.get();
        String username = accounts.account(id).username();
        UserAccountService.validatePassword(username, newPassword);
        jdbc.sql("DELETE FROM email_token WHERE user_id = :id").param("id", id).update();
        jdbc.sql("""
                        UPDATE app_user SET password_hash = :hash, email_verified_at = coalesce(email_verified_at, :now)
                        WHERE id = :id
                        """)
                .param("hash", passwordEncoder.encode(newPassword)).param("now", now()).param("id", id).update();
        jdbc.sql("DELETE FROM spring_session WHERE principal_name = :username").param("username", username).update();
        return Optional.of(username);
    }

    public int deleteExpired() {
        return jdbc.sql("DELETE FROM email_token WHERE expires_at < :now").param("now", now()).update();
    }

    /** User id of a valid (unexpired) token, without using it up. */
    private Optional<Long> userFor(String token, TokenPurpose purpose) {
        if (token == null || token.length() < 40 || token.length() > 64) {
            return Optional.empty();
        }
        return jdbc.sql("""
                        SELECT user_id FROM email_token
                        WHERE token_hash = :hash AND purpose = :purpose AND expires_at > :now
                        """)
                .param("hash", Hashes.sha256(token)).param("purpose", purpose.name()).param("now", now())
                .query(Long.class).optional();
    }

    private Timestamp now() {
        return Timestamp.from(clock.instant());
    }
}
