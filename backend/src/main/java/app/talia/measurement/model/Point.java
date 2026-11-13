package app.talia.measurement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Point(LocalDate date, BigDecimal value) {
}
