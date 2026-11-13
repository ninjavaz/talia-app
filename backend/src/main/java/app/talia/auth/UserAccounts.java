package app.talia.auth;

import app.talia.common.BadRequestException;
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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Accounts: creation with password rules, lookup for login, personal sync tokens. */
@Service
public class UserAccounts implements UserDetailsService {

    @ResponseStatus(HttpStatus.CONFLICT)
    static class UsernameTakenException extends RuntimeException {
        UsernameTakenException() {
            super("Username is already taken");
        }
    }

    static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,30}");
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
    private final SecureRandom random = new SecureRandom();

    UserAccounts(JdbcClient jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    /** Usernames are case-insensitive ("Ola" and "ola" are the same account). */
    static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /** Creates the account with its default settings and the built-in waist measurement. */
    @Transactional
    public TaliaUser create(String rawUsername, String password) {
        String username = normalize(rawUsername);
        if (!USERNAME.matcher(username).matches()) {
            throw new BadRequestException("Username must be 3-30 characters: letters, digits, . _ -");
        }
        validatePassword(username, password);
        long id;
        try {
            id = jdbc.sql("INSERT INTO app_user (username, password_hash) VALUES (:username, :hash) RETURNING id")
                    .param("username", username)
                    .param("hash", passwordEncoder.encode(password))
                    .query(Long.class)
                    .single();
        } catch (DuplicateKeyException e) {
            throw new UsernameTakenException();
        }
        jdbc.sql("INSERT INTO user_settings (user_id) VALUES (:id)").param("id", id).update();
        jdbc.sql("INSERT INTO body_part (user_id, name, code, sort_order) VALUES (:id, 'Talia', 'WAIST', 0)")
                .param("id", id).update();
        return new TaliaUser(id, username, null);
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

    /** Generates a new personal sync token (shown once), stores only its SHA-256. Replaces the previous one. */
    public String newSyncToken(long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.sql("UPDATE app_user SET sync_token_hash = :hash WHERE id = :id")
                .param("hash", ProofOfWork.sha256(token)).param("id", userId).update();
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
}
