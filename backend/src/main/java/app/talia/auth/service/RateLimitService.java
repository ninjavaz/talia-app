package app.talia.auth.service;

import app.talia.auth.model.RateLimit;
import app.talia.auth.model.TooManyRequestsException;
import app.talia.common.Hashes;
import app.talia.config.TaliaProperties;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import io.github.bucket4j.postgresql.PostgreSQLSelectForUpdateBasedProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

/**
 * Rate limits with Bucket4j (token buckets), stored in Postgres so they survive restarts.
 * Each limit has a capacity that refills gradually over its window, e.g. 8 failed logins per 15 minutes.
 * Bucket ids are SHA-256 hashes, so IP and e-mail addresses are not kept in clear text.
 */
@Component
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final PostgreSQLSelectForUpdateBasedProxyManager<String> buckets;
    private final Map<RateLimit, BucketConfiguration> configurations = new EnumMap<>(RateLimit.class);

    RateLimitService(DataSource dataSource, TaliaProperties properties) {
        this.buckets = Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
                .primaryKeyMapper(PrimaryKeyMapper.STRING)
                .table("rate_limit_bucket")
                .idColumn("id")
                .stateColumn("state")
                .expiresAtColumn("expires_at")
                // a bucket that refilled completely holds no information any more and may be deleted
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
                .build();
        var s = properties.security();
        configurations.put(RateLimit.LOGIN_IP, limit(s.loginMaxFailuresPerIp(), s.loginWindow()));
        configurations.put(RateLimit.LOGIN_USER, limit(s.loginMaxFailuresPerUser(), s.loginWindow()));
        configurations.put(RateLimit.REGISTER_IP, limit(s.registrationsPerIp(), s.registrationWindow()));
        configurations.put(RateLimit.CHALLENGE_IP, limit(s.challengesPerIp(), s.registrationWindow()));
        configurations.put(RateLimit.MAIL_IP, limit(s.mailsPerIp(), s.mailWindow()));
        configurations.put(RateLimit.MAIL_ADDRESS, limit(s.mailsPerAddress(), s.mailWindow()));
    }

    private static BucketConfiguration limit(int capacity, Duration window) {
        return BucketConfiguration.builder()
                .addLimit(l -> l.capacity(capacity).refillGreedy(capacity, window))
                .build();
    }

    /** Takes one token or throws 429 (registrations, challenges, e-mails). */
    public void acquire(RateLimit limit, String key) {
        ConsumptionProbe probe = bucket(limit, key).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(seconds(probe.getNanosToWaitForRefill()));
        }
    }

    /** Takes one token if available; false when the limit is used up (nothing thrown, e.g. silent e-mail skip). */
    public boolean tryAcquire(RateLimit limit, String key) {
        return bucket(limit, key).tryConsume(1);
    }

    /** Throws 429 if the limit is used up, without taking a token (checked before a login attempt). */
    public void check(RateLimit limit, String key) {
        var estimate = bucket(limit, key).estimateAbilityToConsume(1);
        if (!estimate.canBeConsumed()) {
            throw new TooManyRequestsException(seconds(estimate.getNanosToWaitForRefill()));
        }
    }

    /** Counts a failure (a failed login). */
    public void record(RateLimit limit, String key) {
        bucket(limit, key).tryConsume(1);
    }

    public void reset(RateLimit limit, String key) {
        buckets.removeProxy(id(limit, key));
    }

    /** Deletes buckets that are full again (they would behave exactly like a missing bucket). */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void removeExpired() {
        int removed;
        int total = 0;
        do {
            removed = buckets.removeExpired(1000);
            total += removed;
        } while (removed == 1000);
        if (total > 0) {
            log.debug("Removed {} expired rate-limit buckets", total);
        }
    }

    private BucketProxy bucket(RateLimit limit, String key) {
        return buckets.builder().build(id(limit, key), () -> configurations.get(limit));
    }

    private static String id(RateLimit limit, String key) {
        return Hashes.sha256(limit.name() + ":" + key);
    }

    private static long seconds(long nanos) {
        return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanos) + 1);
    }
}
