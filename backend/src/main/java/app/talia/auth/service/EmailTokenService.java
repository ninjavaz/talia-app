package app.talia.auth.service;

import app.talia.auth.entity.AppUserEntity;
import app.talia.auth.entity.EmailTokenEntity;
import app.talia.auth.model.TokenPurpose;
import app.talia.auth.repository.AppUserRepository;
import app.talia.auth.repository.EmailTokenRepository;
import app.talia.common.Hashes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * One-time tokens of the e-mail links (confirm address, reset password). Only their SHA-256 is stored;
 * a new token replaces the earlier ones of the same purpose.
 */
@Service
@Transactional
public class EmailTokenService {

    private final EmailTokenRepository tokens;
    private final AppUserRepository users;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    EmailTokenService(EmailTokenRepository tokens, AppUserRepository users,
                      FindByIndexNameSessionRepository<? extends Session> sessions, PasswordEncoder passwordEncoder,
                      Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** A new one-time token for an e-mail link; earlier tokens of the same purpose stop working. */
    public String newToken(long userId, TokenPurpose purpose, Duration ttl) {
        tokens.deleteByUser(userId, purpose);
        String token = Hashes.randomToken();
        Instant now = clock.instant();
        tokens.save(new EmailTokenEntity(Hashes.sha256(token), userId, purpose, now.plus(ttl), now));
        return token;
    }

    /** Confirms the e-mail address of the token's account; false if the link is invalid or expired. */
    public boolean verifyEmail(String token) {
        Optional<Long> userId = userFor(token, TokenPurpose.VERIFY);
        userId.ifPresent(id -> {
            tokens.deleteByUser(id, TokenPurpose.VERIFY);
            users.findById(id).orElseThrow().markEmailVerified(clock.instant());
        });
        return userId.isPresent();
    }

    /**
     * Sets a new password from a reset link. Also confirms the e-mail (the link proves access to it) and ends all
     * sessions of the account on every device. Returns the username, or empty if the link is invalid or expired.
     */
    public Optional<String> resetPassword(String token, String newPassword) {
        Optional<Long> userId = userFor(token, TokenPurpose.RESET);
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        AppUserEntity user = users.findById(userId.get()).orElseThrow();
        UserAccountService.validatePassword(user.getUsername(), newPassword);
        tokens.deleteAllByUser(user.getId());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.markEmailVerified(clock.instant());
        sessions.findByPrincipalName(user.getUsername()).keySet().forEach(sessions::deleteById);
        return Optional.of(user.getUsername());
    }

    public int deleteExpired() {
        return tokens.deleteExpired(clock.instant());
    }

    /** User id of a valid (unexpired) token, without using it up. */
    private Optional<Long> userFor(String token, TokenPurpose purpose) {
        if (token == null || token.length() < 40 || token.length() > 64) {
            return Optional.empty();
        }
        return tokens.findValidUserId(Hashes.sha256(token), purpose, clock.instant());
    }
}
