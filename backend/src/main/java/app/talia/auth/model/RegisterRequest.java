package app.talia.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param altcha  base64 payload from the ALTCHA widget (solved proof of work)
 * @param website honeypot: invisible to people, bots that fill every field give themselves away
 */
public record RegisterRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 300) String email,
                       @NotBlank @Size(max = 200) String password, @NotBlank @Size(max = 4000) String altcha,
                       String website, String language) {
}
