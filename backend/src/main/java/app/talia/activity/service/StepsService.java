package app.talia.activity.service;

import app.talia.activity.repository.WeeklyStepsRepository;
import app.talia.common.Weeks;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/** Average daily steps of a week: the manual weekly value if entered, otherwise the average of synced days. */
@Service
public class StepsService {

    private final WeeklyStepsRepository weeklySteps;

    StepsService(WeeklyStepsRepository weeklySteps) {
        this.weeklySteps = weeklySteps;
    }

    public Optional<Integer> avgDailySteps(long userId, LocalDate anyDayOfWeek) {
        LocalDate start = Weeks.start(anyDayOfWeek);
        return weeklySteps.avgDailySteps(userId, start, start.plusDays(6));
    }
}
