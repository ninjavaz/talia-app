package app.talia.auth.service;

import app.talia.auth.repository.AppUserRepository;
import app.talia.common.Hashes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Personal Apple Health sync tokens: 256 random bits, shown once, only the SHA-256 is stored. A fast hash is
 * enough for that much randomness (unlike passwords), and the lookup is by hash, so the token never touches the DB.
 */
@Service
@Transactional
public class SyncTokenService {

    private final AppUserRepository users;

    SyncTokenService(AppUserRepository users) {
        this.users = users;
    }

    /** Generates a new token (returned only this once). Replaces the previous one. */
    public String newToken(long userId) {
        String token = Hashes.randomToken();
        users.findById(userId).orElseThrow().setSyncTokenHash(Hashes.sha256(token));
        return token;
    }

    @Transactional(readOnly = true)
    public boolean hasToken(long userId) {
        return users.findById(userId).orElseThrow().getSyncTokenHash() != null;
    }

    /** The user id the token belongs to (the hash column is unique and indexed). */
    @Transactional(readOnly = true)
    public Optional<Long> userIdFor(String token) {
        if (token == null || token.length() < 20) {
            return Optional.empty();
        }
        return users.findIdBySyncTokenHash(Hashes.sha256(token));
    }
}
