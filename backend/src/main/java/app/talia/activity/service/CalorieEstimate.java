package app.talia.activity.service;

import app.talia.activity.model.WorkoutCategory;

import java.math.BigDecimal;

/** REQ-022: estimated burned kcal of a workout. */
public final class CalorieEstimate {

    private CalorieEstimate() {
    }

    public static Integer kcal(WorkoutCategory category, int durationMin, BigDecimal weightKg) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
