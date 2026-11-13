package app.talia.auth.service;

import app.talia.common.Hashes;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Personal Apple Health sync tokens: 256 random bits, shown once, only the SHA-256 is stored. A fast hash is
 * enough for that much randomness (unlike passwords), and the lookup is by hash, so the token never touches the DB.
 */
@Service
public class SyncTokenService {

    private final JdbcClient jdbc;

    SyncTokenService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Generates a new token (returned only this once). Replaces the previous one. */
    public String newToken(long userId) {
        String token = Hashes.randomToken();
        jdbc.sql("UPDATE app_user SET sync_token_hash = :hash WHERE id = :id")
                .param("hash", Hashes.sha256(token)).param("id", userId).update();
        return token;
    }

    public boolean hasToken(long userId) {
        return jdbc.sql("SELECT sync_token_hash IS NOT NULL FROM app_user WHERE id = :id")
                .param("id", userId).query(Boolean.class).single();
    }

    /** The user id the token belongs to (the hash column is unique and indexed). */
    public Optional<Long> userIdFor(String token) {
        if (token == null || token.length() < 20) {
            return Optional.empty();
        }
        return jdbc.sql("SELECT id FROM app_user WHERE sync_token_hash = :hash")
                .param("hash", Hashes.sha256(token)).query(Long.class).optional();
    }
}
