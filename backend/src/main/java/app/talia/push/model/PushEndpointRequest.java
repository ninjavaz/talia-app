package app.talia.push.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identifies one subscription of the current user (to unsubscribe this device). */
public record PushEndpointRequest(@NotBlank @Size(max = 1000) String endpoint) {
}
