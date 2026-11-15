package app.talia.measurement.model;

import java.time.LocalDate;
import java.util.List;

public record ProgressView(
        Metric weight,
        List<BodyPartProgress> bodyParts,
        List<Point> weightPoints,
        Integer avgDailyStepsThisWeek,
        Integer avgDailyStepsLastWeek,
        int dailyStepsGoal,
        LocalDate lastCheckin,
        boolean checkinDoneThisWeek,
        boolean checkinDue) {
}
