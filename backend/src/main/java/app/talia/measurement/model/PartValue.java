package app.talia.measurement.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PartValue(@NotNull Long partId, @NotNull @DecimalMin("5") @DecimalMax("300") BigDecimal valueCm) {
}
