package app.talia.auth;

import app.talia.common.BadRequestException;
import app.talia.common.Hashes;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Accounts: creation with password rules, lookup for login, e-mail links, personal sync tokens. */
@Service
public class UserAccounts implements UserDetailsService {

    @ResponseStatus(HttpStatus.CONFLICT)
    static class UsernameTakenException extends RuntimeException {
        UsernameTakenException() {
            super("Username is already taken");
        }
    }

    /** The e-mail address belongs to another account (the caller answers as if it worked, see AuthController). */
    static class EmailTakenException extends RuntimeException {
        EmailTakenException() {
            super("E-mail is already used");
        }
    }

    enum TokenPurpose { VERIFY, RESET }

    /** Account data needed by the e-mail flows. */
    record Account(long id, String username, String email, String language, boolean emailVerified) {
    }

    static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,30}");
    /** Pragmatic check (no spaces or control characters, one @, a dot in the domain); the e-mail link proves the rest. */
    static final Pattern EMAIL = Pattern.compile("[^@\\s\\p{Cntrl}]{1,64}@[^@\\s\\p{Cntrl}]+\\.[^@\\s\\p{Cntrl}]{2,}");
    static final int MAX_EMAIL_LENGTH = 254;
    static final int MIN_PASSWORD_LENGTH = 10;
    /** bcrypt only uses the first 72 bytes; longer passwords are rejected instead of silently truncated. */
    static final int MAX_PASSWORD_BYTES = 72;

    /** A tiny deny-list of the most common passwords that pass the length rule (NIST SP 800-63B). */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "1234567890", "12345678910", "123456789a", "qwertyuiop", "qwerty1234", "password12", "password123",
            "1q2w3e4r5t", "1qaz2wsx3edc", "zaq12wsxcde", "qazwsxedcrfv", "haslo12345", "haslohaslo", "iloveyou12",
            "0987654321", "1111111111", "aaaaaaaaaa", "abcdefghij", "abc1234567", "qwerty12345");

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    UserAccounts(JdbcClient jdbc, PasswordEncoder passwordEncoder, Clock clock) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Usernames are case-insensitive ("Ola" and "ola" are the same account). */
    static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /** E-mail addresses are compared case-insensitively. */
    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isValidEmail(String normalizedEmail) {
        return normalizedEmail.length() <= MAX_EMAIL_LENGTH && EMAIL.matcher(normalizedEmail).matches();
    }

    /** Creates an account with a verified e-mail (tests, local tools). */
    public TaliaUser create(String username, String password) {
        TaliaUser user = create(username, normalize(username) + "@example.com", password, "pl");
        jdbc.sql("UPDATE app_user SET email_verified_at = :now WHERE id = :id")
                .param("now", now()).param("id", user.id()).update();
        return user;
    }

    /**
     * Creates the account with its default settings and the built-in waist measurement.
     * The e-mail is unverified: login is possible after clicking the link from the e-mail.
     */
    @Transactional
    public TaliaUser create(String rawUsername, String rawEmail, String password, String language) {
        String username = normalize(rawUsername);
        String email = normalizeEmail(rawEmail);
        if (!USERNAME.matcher(username).matches()) {
            throw new BadRequestException("Username must be 3-30 characters: letters, digits, . _ -");
        }
        if (!isValidEmail(email)) {
            throw new BadRequestException("Invalid e-mail address");
        }
        validatePassword(username, password);
        // hashed before the checks below, so "address already used" takes as long as a real registration
        String hash = passwordEncoder.encode(password);
        if (exists("username", username)) {
            throw new UsernameTakenException();
        }
        if (exists("email", email)) {
            throw new EmailTakenException();
        }
        long id;
        try {
            id = jdbc.sql("""
                            INSERT INTO app_user (username, email, password_hash, created_at)
                            VALUES (:username, :email, :hash, :now) RETURNING id
                            """)
                    .param("username", username)
                    .param("email", email)
                    .param("hash", hash)
                    .param("now", now())
                    .query(Long.class)
                    .single();
        } catch (DuplicateKeyException e) {
            // two registrations at the same moment
            throw new UsernameTakenException();
        }
        jdbc.sql("INSERT INTO user_settings (user_id, language) VALUES (:id, :language)")
                .param("id", id).param("language", "en".equals(language) ? "en" : "pl").update();
        jdbc.sql("INSERT INTO body_part (user_id, name, code, sort_order) VALUES (:id, 'Talia', 'WAIST', 0)")
                .param("id", id).update();
        return new TaliaUser(id, username, null);
    }

    private boolean exists(String column, String value) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM app_user WHERE " + column + " = :value)")
                .param("value", value).query(Boolean.class).single();
    }

    static void validatePassword(String username, String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException("Password must have at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BadRequestException("Password is too long");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (lower.contains(username) || COMMON_PASSWORDS.contains(lower) || lower.chars().distinct().count() < 4) {
            throw new BadRequestException("Password is too easy to guess");
        }
    }

    @Override
    public TaliaUser loadUserByUsername(String username) {
        return jdbc.sql("SELECT id, username, password_hash FROM app_user WHERE username = :username")
                .param("username", normalize(username))
                .query((rs, n) -> new TaliaUser(rs.getLong(1), rs.getString(2), rs.getString(3)))
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    private static final String ACCOUNT_SELECT = """
            SELECT u.id, u.username, u.email, s.language, u.email_verified_at IS NOT NULL
            FROM app_user u JOIN user_settings s ON s.user_id = u.id
            """;

    Account account(long userId) {
        return jdbc.sql(ACCOUNT_SELECT + " WHERE u.id = :id").param("id", userId)
                .query((rs, n) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBoolean(5)))
                .single();
    }

    Optional<Account> accountByEmail(String email) {
        return jdbc.sql(ACCOUNT_SELECT + " WHERE u.email = :email").param("email", normalizeEmail(email))
                .query((rs, n) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBoolean(5)))
                .optional();
    }

    // E-mail links ------------------------------------------------------------------------------------------------

    /** A new one-time token for an e-mail link; earlier tokens of the same purpose stop working. */
    @Transactional
    String newEmailToken(long userId, TokenPurpose purpose, Duration ttl) {
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

    /** User id of a valid (unexpired) token, without using it up. */
    Optional<Long> userForEmailToken(String token, TokenPurpose purpose) {
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

    /** Confirms the e-mail address of the token's account; false if the link is invalid or expired. */
    @Transactional
    boolean verifyEmail(String token) {
        Optional<Long> userId = userForEmailToken(token, TokenPurpose.VERIFY);
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
    Optional<String> resetPassword(String token, String newPassword) {
        Optional<Long> userId = userForEmailToken(token, TokenPurpose.RESET);
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        long id = userId.get();
        String username = account(id).username();
        validatePassword(username, newPassword);
        jdbc.sql("DELETE FROM email_token WHERE user_id = :id").param("id", id).update();
        jdbc.sql("""
                        UPDATE app_user SET password_hash = :hash, email_verified_at = coalesce(email_verified_at, :now)
                        WHERE id = :id
                        """)
                .param("hash", passwordEncoder.encode(newPassword)).param("now", now()).param("id", id).update();
        jdbc.sql("DELETE FROM spring_session WHERE principal_name = :username").param("username", username).update();
        return Optional.of(username);
    }

    /** Deletes accounts whose e-mail was never confirmed (frees the username and address again). */
    int deleteUnverifiedOlderThan(Duration age) {
        return jdbc.sql("DELETE FROM app_user WHERE email_verified_at IS NULL AND created_at < :cutoff")
                .param("cutoff", Timestamp.from(clock.instant().minus(age))).update();
    }

    int deleteExpiredEmailTokens() {
        return jdbc.sql("DELETE FROM email_token WHERE expires_at < :now").param("now", now()).update();
    }

    // Sync tokens -------------------------------------------------------------------------------------------------

    /** Generates a new personal sync token (shown once), stores only its SHA-256. Replaces the previous one. */
    public String newSyncToken(long userId) {
        String token = Hashes.randomToken();
        jdbc.sql("UPDATE app_user SET sync_token_hash = :hash WHERE id = :id")
                .param("hash", Hashes.sha256(token)).param("id", userId).update();
        return token;
    }

    public boolean hasSyncToken(long userId) {
        return jdbc.sql("SELECT sync_token_hash IS NOT NULL FROM app_user WHERE id = :id")
                .param("id", userId).query(Boolean.class).single();
    }

    /** User id for a sync token, looked up by its hash (the hash column is unique and indexed). */
    Long userIdForSyncTokenHash(String sha256Hex) {
        return jdbc.sql("SELECT id FROM app_user WHERE sync_token_hash = :hash")
                .param("hash", sha256Hex).query(Long.class).optional().orElse(null);
    }

    private Timestamp now() {
        return Timestamp.from(clock.instant());
    }
}
