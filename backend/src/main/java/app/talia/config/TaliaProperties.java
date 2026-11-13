package app.talia.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Validated
@ConfigurationProperties("talia")
public record TaliaProperties(
        @NotNull ZoneId timezone,
        @Valid @NotNull Auth auth,
        @Valid @NotNull Sync sync) {

    public record Auth(@NotBlank String username, @NotBlank String passwordHash) {
    }

    public record Sync(@NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String tokenSha256) {
    }
}
