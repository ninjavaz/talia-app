package app.talia.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank @Size(max = 100) String token, @NotBlank @Size(max = 200) String password) {
}
