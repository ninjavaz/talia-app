package app.talia.auth.service;

import app.talia.auth.entity.AppUserEntity;
import app.talia.auth.model.Account;
import app.talia.auth.model.EmailTakenException;
import app.talia.auth.model.TaliaUser;
import app.talia.auth.model.UsernameTakenException;
import app.talia.auth.repository.AppUserRepository;
import app.talia.common.BadRequestException;
import app.talia.settings.service.SettingsService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Accounts: creation with the username, e-mail and password rules, lookup for login and the e-mail flows. */
@Service
@Transactional
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

    private final AppUserRepository users;
    private final SettingsService settings;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    UserAccountService(AppUserRepository users, SettingsService settings, PasswordEncoder passwordEncoder,
                       Clock clock) {
        this.users = users;
        this.settings = settings;
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
        users.findById(user.id()).orElseThrow().markEmailVerified(clock.instant());
        return user;
    }

    /**
     * Creates the account with its default settings (no body parts: the user adds what they want to measure).
     * The e-mail is unverified: login is possible after clicking the link from the e-mail.
     */
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
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException();
        }
        if (users.existsByEmail(email)) {
            throw new EmailTakenException();
        }
        AppUserEntity user;
        try {
            user = users.saveAndFlush(new AppUserEntity(username, email, hash, clock.instant()));
        } catch (DataIntegrityViolationException e) {
            // two registrations at the same moment
            throw new UsernameTakenException();
        }
        settings.createDefaults(user.getId(), "en".equals(language) ? "en" : "pl");
        return new TaliaUser(user.getId(), username, null);
    }

    @Override
    @Transactional(readOnly = true)
    public TaliaUser loadUserByUsername(String username) {
        return users.findByUsername(normalize(username))
                .map(u -> new TaliaUser(u.getId(), u.getUsername(), u.getPasswordHash()))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    @Transactional(readOnly = true)
    public Account account(long userId) {
        return toAccount(users.findById(userId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public Optional<Account> accountByEmail(String email) {
        return users.findByEmail(normalizeEmail(email)).map(this::toAccount);
    }

    /** Deletes accounts whose e-mail was never confirmed (frees the username and address again). */
    public int deleteUnverifiedOlderThan(Duration age) {
        return users.deleteUnverifiedCreatedBefore(clock.instant().minus(age));
    }

    private Account toAccount(AppUserEntity u) {
        return new Account(u.getId(), u.getUsername(), u.getEmail(), settings.get(u.getId()).language(),
                u.isEmailVerified());
    }
}
