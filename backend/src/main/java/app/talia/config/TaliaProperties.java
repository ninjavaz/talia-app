package app.talia.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

@Validated
@ConfigurationProperties("talia")
public record TaliaProperties(@NotNull ZoneId timezone, @Valid @NotNull Security security) {

    /** Brute-force and bot protection limits (see application.yml for the meaning). */
    public record Security(
            @Min(1) int loginMaxFailuresPerIp,
            @Min(1) int loginMaxFailuresPerUser,
            @NotNull Duration loginWindow,
            @Min(1) int registrationsPerIp,
            @NotNull Duration registrationWindow,
            @Min(1) int challengesPerIp,
            @Min(100) int powMaxNumber,
            @NotNull Duration powTtl,
            @NotNull Boolean cookieSecure) {
    }
}
