package app.talia.auth.service;

import app.talia.auth.model.Account;
import app.talia.auth.model.EmailTakenException;
import app.talia.auth.model.TaliaUser;
import app.talia.auth.model.UsernameTakenException;
import app.talia.common.BadRequestException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Accounts: creation with the username, e-mail and password rules, lookup for login and the e-mail flows. */
@Service
public class UserAccountService implements UserDetailsService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,30}");
    /** Pragmatic check (no spaces or control characters, one @, a dot in the domain); the e-mail link proves the rest. */
    private static final Pattern EMAIL = Pattern.compile("[^@\\s\\p{Cntrl}]{1,64}@[^@\\s\\p{Cntrl}]+\\.[^@\\s\\p{Cntrl}]{2,}");
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MIN_PASSWORD_LENGTH = 10;
    /** bcrypt only uses the first 72 bytes; longer passwords are rejected instead of silently truncated. */
    private static final int MAX_PASSWORD_BYTES = 72;

    /** A tiny deny-list of the most common passwords that pass the length rule (NIST SP 800-63B). */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "1234567890", "12345678910", "123456789a", "qwertyuiop", "qwerty1234", "password12", "password123",
            "1q2w3e4r5t", "1qaz2wsx3edc", "zaq12wsxcde", "qazwsxedcrfv", "haslo12345", "haslohaslo", "iloveyou12",
            "0987654321", "1111111111", "aaaaaaaaaa", "abcdefghij", "abc1234567", "qwerty12345");

    private static final String ACCOUNT_SELECT = """
            SELECT u.id, u.username, u.email, s.language, u.email_verified_at IS NOT NULL
            FROM app_user u JOIN user_settings s ON s.user_id = u.id
            """;

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    UserAccountService(JdbcClient jdbc, PasswordEncoder passwordEncoder, Clock clock) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Usernames are case-insensitive ("Ola" and "ola" are the same account). */
    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /** E-mail addresses are compared case-insensitively. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String normalizedEmail) {
        return normalizedEmail.length() <= MAX_EMAIL_LENGTH && EMAIL.matcher(normalizedEmail).matches();
    }

    public static void validatePassword(String username, String password) {
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

    /** Creates an account with a verified e-mail (tests, local tools). */
    public TaliaUser create(String username, String password) {
        TaliaUser user = create(username, normalize(username) + "@example.com", password, "pl");
        jdbc.sql("UPDATE app_user SET email_verified_at = :now WHERE id = :id")
                .param("now", now()).param("id", user.id()).update();
        return user;
    }

    /**
     * Creates the account with its default settings (no body parts: the user adds what they want to measure).
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
        return new TaliaUser(id, username, null);
    }

    @Override
    public TaliaUser loadUserByUsername(String username) {
        return jdbc.sql("SELECT id, username, password_hash FROM app_user WHERE username = :username")
                .param("username", normalize(username))
                .query((rs, n) -> new TaliaUser(rs.getLong(1), rs.getString(2), rs.getString(3)))
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    public Account account(long userId) {
        return jdbc.sql(ACCOUNT_SELECT + " WHERE u.id = :id").param("id", userId)
                .query((rs, n) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBoolean(5)))
                .single();
    }

    public Optional<Account> accountByEmail(String email) {
        return jdbc.sql(ACCOUNT_SELECT + " WHERE u.email = :email").param("email", normalizeEmail(email))
                .query((rs, n) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBoolean(5)))
                .optional();
    }

    /** Deletes accounts whose e-mail was never confirmed (frees the username and address again). */
    public int deleteUnverifiedOlderThan(Duration age) {
        return jdbc.sql("DELETE FROM app_user WHERE email_verified_at IS NULL AND created_at < :cutoff")
                .param("cutoff", Timestamp.from(clock.instant().minus(age))).update();
    }

    private boolean exists(String column, String value) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM app_user WHERE " + column + " = :value)")
                .param("value", value).query(Boolean.class).single();
    }

    private Timestamp now() {
        return Timestamp.from(clock.instant());
    }
}
