package app.talia.measurement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BodyPartRequest(@NotBlank @Size(max = 50) String name) {
}
