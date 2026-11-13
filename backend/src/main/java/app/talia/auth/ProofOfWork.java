package app.talia.auth;

import app.talia.config.TaliaProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Self-hosted, privacy-friendly bot check for registration (proof of work in the ALTCHA format, no third party).
 * <p>
 * The server picks a secret random number n and sends {@code challenge = SHA-256(salt + n)} with an HMAC signature.
 * The browser must find n by brute force (on average maxNumber/2 hashes - about a second on a phone, but expensive
 * for a bot creating many accounts). The answer is verified statelessly via the HMAC, must not be expired and can be
 * used only once.
 */
@Component
class ProofOfWork {

    record Challenge(String algorithm, String challenge, String salt, int maxNumber, String signature) {
    }

    record Solution(String algorithm, String challenge, String salt, Integer number, String signature) {
    }

    private static final String ALGORITHM = "SHA-256";
    private static final HexFormat HEX = HexFormat.of();

    private final SecureRandom random = new SecureRandom();
    /** Per-process secret: challenges issued before a restart simply become invalid. */
    private final byte[] hmacKey = new byte[32];
    private final Map<String, Instant> usedChallenges = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxNumber;
    private final Duration ttl;

    ProofOfWork(Clock clock, TaliaProperties properties) {
        this.clock = clock;
        this.maxNumber = properties.security().powMaxNumber();
        this.ttl = properties.security().powTtl();
        random.nextBytes(hmacKey);
    }

    Challenge create() {
        byte[] saltBytes = new byte[12];
        random.nextBytes(saltBytes);
        long expires = clock.instant().plus(ttl).getEpochSecond();
        String salt = HEX.formatHex(saltBytes) + "?expires=" + expires;
        int number = random.nextInt(maxNumber + 1);
        String challenge = sha256(salt + number);
        return new Challenge(ALGORITHM, challenge, salt, maxNumber, hmac(challenge));
    }

    boolean verify(Solution s) {
        if (s == null || s.number() == null || s.salt() == null || s.challenge() == null || s.signature() == null
                || !ALGORITHM.equals(s.algorithm())) {
            return false;
        }
        Instant now = clock.instant();
        Long expires = parseExpires(s.salt());
        if (expires == null || now.getEpochSecond() > expires) {
            return false;
        }
        boolean valid = MessageDigest.isEqual(bytes(hmac(s.challenge())), bytes(s.signature()))
                && MessageDigest.isEqual(bytes(sha256(s.salt() + s.number())), bytes(s.challenge()));
        if (!valid) {
            return false;
        }
        usedChallenges.values().removeIf(exp -> exp.isBefore(now));
        // single use: a solved challenge cannot be replayed for another account
        return usedChallenges.putIfAbsent(s.challenge(), Instant.ofEpochSecond(expires)) == null;
    }

    private static Long parseExpires(String salt) {
        int i = salt.indexOf("?expires=");
        if (i < 0) {
            return null;
        }
        try {
            return Long.parseLong(salt.substring(i + "?expires=".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HEX.formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static String sha256(String value) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
