package app.talia.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * @param publicUrl address of the app as users open it (links in e-mails), without a trailing slash
 */
@Validated
@ConfigurationProperties("talia")
public record TaliaProperties(@NotNull ZoneId timezone, @NotBlank String publicUrl, @Valid @NotNull Mail mail,
                              @Valid @NotNull Security security, @Valid @NotNull Push push) {

    /**
     * @param from sender of account e-mails, e.g. {@code Talia <no-reply@example.com>}
     */
    public record Mail(@NotBlank String from) {
    }

    /** Brute-force and bot protection (see application.yml for the meaning). */
    public record Security(
            @Min(1) int loginMaxFailuresPerIp,
            @Min(1) int loginMaxFailuresPerUser,
            @NotNull Duration loginWindow,
            @Min(1) int registrationsPerIp,
            @NotNull Duration registrationWindow,
            @Min(1) int challengesPerIp,
            @Min(1) int mailsPerIp,
            @Min(1) int mailsPerAddress,
            @NotNull Duration mailWindow,
            @NotNull Duration verifyLinkTtl,
            @NotNull Duration resetLinkTtl,
            @Valid @NotNull Altcha altcha,
            @NotNull Boolean cookieSecure) {
    }

    /**
     * ALTCHA proof of work (PBKDF2/SHA-256). The browser tries on average {@code maxCounter * 3/4} counters,
     * each costing {@code cost} PBKDF2 iterations.
     *
     * @param hmacSecret signs challenges; empty = random per start (open challenges then die with a restart)
     */
    public record Altcha(@Min(1) int cost, @Min(10) int maxCounter, @NotNull Duration ttl, String hmacSecret) {
    }

    /**
     * Web Push (REQ-030..034).
     *
     * @param vapidPublicKey          base64url uncompressed P-256 point; empty = random pair per start (dev only)
     * @param vapidPrivateKey         base64url 32-byte private scalar
     * @param vapidSubject            contact of the operator for the push services ({@code mailto:} or https URL)
     * @param allowedHostSuffixes     push services the server may send to (SSRF guard); a host matches a suffix when
     *                                it equals it or ends with "." + suffix
     * @param allowHttp               plain http endpoints (tests only)
     * @param maxSubscriptionsPerUser devices per user; the oldest is dropped beyond this
     */
    public record Push(String vapidPublicKey, String vapidPrivateKey, @NotBlank String vapidSubject,
                       @NotEmpty List<String> allowedHostSuffixes, boolean allowHttp,
                       @Min(1) int maxSubscriptionsPerUser) {
    }
}
