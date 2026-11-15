package app.talia.push.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The browser's {@code PushSubscription.toJSON()} (REQ-033). */
public record PushSubscriptionRequest(@NotBlank @Size(max = 1000) String endpoint, @Valid @NotNull Keys keys) {

    /** Base64url: {@code p256dh} = device public key, {@code auth} = 16-byte authentication secret. */
    public record Keys(@NotBlank @Size(max = 200) String p256dh, @NotBlank @Size(max = 100) String auth) {
    }
}
