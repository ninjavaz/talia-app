package app.talia.activity.service;

import app.talia.activity.model.WorkoutCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * REQ-022: estimated burned kcal of a workout = MET x body weight (kg) x duration (h). MET values from the Compendium
 * of Physical Activities (resistance training ~3.5-6, pilates ~3.0, walking at a moderate pace ~3.5); "other" takes
 * a middle value.
 */
public final class CalorieEstimate {

    private static final Map<WorkoutCategory, BigDecimal> MET = new EnumMap<>(Map.of(
            WorkoutCategory.GYM, new BigDecimal("5.0"),
            WorkoutCategory.PILATES, new BigDecimal("3.0"),
            WorkoutCategory.WALK, new BigDecimal("3.5"),
            WorkoutCategory.OTHER, new BigDecimal("4.0")));

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private CalorieEstimate() {
    }

    /** Whole kcal, or null without a weight. */
    public static Integer kcal(WorkoutCategory category, int durationMin, BigDecimal weightKg) {
        if (weightKg == null) {
            return null;
        }
        return MET.get(category)
                .multiply(weightKg)
                .multiply(BigDecimal.valueOf(durationMin))
                .divide(MINUTES_PER_HOUR, 0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}
