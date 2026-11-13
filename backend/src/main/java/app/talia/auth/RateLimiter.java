package app.talia.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window counter per key (e.g. "ip:1.2.3.4"). Enough for a single backend instance;
 * the state resets on restart, which only ever makes the limits more lenient for a moment.
 */
final class RateLimiter {

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    RateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /** True if the key already used up its limit in the current window (does not count a hit). */
    boolean isBlocked(String key) {
        Deque<Instant> deque = hits.get(key);
        if (deque == null) {
            return false;
        }
        synchronized (deque) {
            prune(deque);
            return deque.size() >= limit;
        }
    }

    /** Counts a hit; returns false if the key was already over the limit (the hit is still counted). */
    boolean tryAcquire(String key) {
        if (hits.size() > 10_000) {
            cleanup();
        }
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            prune(deque);
            boolean allowed = deque.size() < limit;
            deque.addLast(clock.instant());
            return allowed;
        }
    }

    void reset(String key) {
        hits.remove(key);
    }

    /** Seconds until the oldest hit of the key leaves the window (for Retry-After). */
    long retryAfterSeconds(String key) {
        Deque<Instant> deque = hits.get(key);
        if (deque == null) {
            return 0;
        }
        synchronized (deque) {
            Instant oldest = deque.peekFirst();
            return oldest == null ? 0 : Math.max(1, Duration.between(clock.instant(), oldest.plus(window)).toSeconds());
        }
    }

    /** Drops keys without hits in the window (bounded memory under a flood of distinct IPs). */
    private void cleanup() {
        hits.forEach((key, deque) -> {
            synchronized (deque) {
                prune(deque);
                if (deque.isEmpty()) {
                    hits.remove(key, deque);
                }
            }
        });
    }

    private void prune(Deque<Instant> deque) {
        Instant cutoff = clock.instant().minus(window);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }
}
