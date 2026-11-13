package app.talia.measurement.model;

import java.time.LocalDate;
import java.util.List;

public record ProgressView(
        Metric waist,
        Metric weight,
        List<BodyPartProgress> bodyParts,
        List<Point> weightPoints,
        List<Point> weightTrend,
        Integer avgDailyStepsThisWeek,
        Integer avgDailyStepsLastWeek,
        int dailyStepsGoal,
        LocalDate lastCheckin,
        boolean checkinDoneThisWeek,
        boolean checkinDue) {
}
