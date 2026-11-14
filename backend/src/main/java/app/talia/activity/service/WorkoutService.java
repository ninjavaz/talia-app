package app.talia.activity.service;

import app.talia.activity.entity.WorkoutEntity;
import app.talia.activity.model.Workout;
import app.talia.activity.model.WorkoutCategory;
import app.talia.activity.repository.WorkoutRepository;
import app.talia.common.BadRequestException;
import app.talia.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class WorkoutService {

    private static final LocalTime DEFAULT_PAST_TIME = LocalTime.of(12, 0);

    private final WorkoutRepository workouts;
    private final Clock clock;

    WorkoutService(WorkoutRepository workouts, Clock clock) {
        this.workouts = workouts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Workout> between(long userId, LocalDate from, LocalDate to) {
        return workouts.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAt(
                        userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream()
                .map(WorkoutService::toModel)
                .toList();
    }

    /**
     * Manual workout. Only the day matters to the user; the start time is "now" for today and noon for past days,
     * shifted by a minute if a workout of the same type already starts then (unique key user+start+type).
     */
    public Workout add(long userId, LocalDate date, WorkoutCategory category, int durationMin) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            throw new BadRequestException("Date is in the future");
        }
        LocalTime time = date.equals(today)
                ? LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
                : DEFAULT_PAST_TIME;
        LocalDateTime start = date.atTime(time);
        for (int attempt = 0; attempt < 60; attempt++, start = start.plusMinutes(1)) {
            if (!workouts.existsByUserIdAndStartedAtAndType(userId, start, category.name())) {
                WorkoutEntity saved = workouts.save(new WorkoutEntity(userId, start, start.plusMinutes(durationMin),
                        category.name(), category));
                return toModel(saved);
            }
        }
        throw new BadRequestException("Too many workouts of this type on that day");
    }

    public void delete(long userId, long id) {
        if (workouts.deleteByIdAndUserId(id, userId) == 0) {
            throw new NotFoundException("Workout not found");
        }
    }

    private static Workout toModel(WorkoutEntity e) {
        return new Workout(e.getId(), e.getStartedAt(),
                (int) Duration.between(e.getStartedAt(), e.getEndedAt()).toMinutes(), e.getType(), e.getCategory(),
                e.getKcal());
    }
}
