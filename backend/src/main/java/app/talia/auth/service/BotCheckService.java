package app.talia.auth.service;

import app.talia.auth.repository.AltchaUsedRepository;
import app.talia.common.Hashes;
import app.talia.config.TaliaProperties;
import org.altcha.altcha.v2.Altcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

/**
 * Bot check at registration with the official ALTCHA library (protocol v2, PBKDF2/SHA-256, deterministic mode).
 * The server picks a secret counter and signs the challenge; the browser (ALTCHA widget) has to find the counter by
 * brute force, which takes a person a second once and a bot farm a lot of CPU for every account.
 * Each solved challenge is accepted once (tracked in {@code altcha_used}).
 */
@Service
public class BotCheckService {

    private static final Logger log = LoggerFactory.getLogger(BotCheckService.class);
    private static final String ALGORITHM = "PBKDF2/SHA-256";

    private final TaliaProperties.Altcha config;
    private final String hmacSecret;
    private final String keySecret;
    private final AltchaUsedRepository used;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    BotCheckService(TaliaProperties properties, AltchaUsedRepository used, Clock clock) {
        this.config = properties.security().altcha();
        String configured = config.hmacSecret();
        // both secrets derive from the configured one, or are random for this process
        String base = configured == null || configured.isBlank() ? Hashes.randomToken() : configured;
        this.hmacSecret = Hashes.sha256("challenge:" + base);
        this.keySecret = Hashes.sha256("key:" + base);
        this.used = used;
        this.clock = clock;
    }

    /** A new signed challenge as JSON for the widget. */
    public String createChallenge() {
        int max = config.maxCounter();
        long counter = max / 2 + random.nextInt(max - max / 2);
        try {
            return Altcha.createChallenge(new Altcha.CreateChallengeOptions()
                    .algorithm(ALGORITHM)
                    .cost(config.cost())
                    .counter(counter)
                    .expiresInSeconds(config.ttl().toSeconds())
                    .hmacSignatureSecret(hmacSecret)
                    .hmacKeySignatureSecret(keySecret)).toJson();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create an ALTCHA challenge", e);
        }
    }

    /** True if the payload is a valid, unexpired solution that was not used before (and marks it as used). */
    @Transactional
    public boolean verify(String base64Payload) {
        if (base64Payload == null || base64Payload.isBlank() || base64Payload.length() > 4000) {
            return false;
        }
        Altcha.Payload payload;
        Altcha.VerifySolutionResult result;
        try {
            payload = Altcha.parsePayload(base64Payload);
            result = Altcha.verifySolution(payload.challenge(), payload.solution(), hmacSecret, keySecret, null);
        } catch (Exception e) {
            log.debug("Invalid ALTCHA payload: {}", e.getMessage());
            return false;
        }
        if (!result.verified()) {
            return false;
        }
        Number expiresAt = payload.challenge().parameters().expiresAt();
        Instant expires = expiresAt == null ? clock.instant().plus(config.ttl())
                : Instant.ofEpochSecond(expiresAt.longValue());
        return used.markUsed(Hashes.sha256(payload.challenge().signature()), expires) == 1;
    }

    /** Expired challenges can not be replayed anyway (the signature check rejects them). */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    @Transactional
    public void removeExpired() {
        used.deleteExpiredBefore(clock.instant().minus(config.ttl()));
    }
}
