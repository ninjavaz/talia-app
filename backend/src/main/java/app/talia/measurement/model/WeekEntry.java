package app.talia.measurement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One week of history. Weight is the average of that week's entries (manual and synced). */
public record WeekEntry(LocalDate weekStart, List<PartValue> measurements, BigDecimal weightKg, Integer avgDailySteps) {
}
