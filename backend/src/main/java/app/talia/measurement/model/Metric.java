package app.talia.measurement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** latest/first are the newest and oldest values; delta = latest - first (negative = less). */
public record Metric(BigDecimal latest, LocalDate latestDate, BigDecimal first, BigDecimal delta) {
}
