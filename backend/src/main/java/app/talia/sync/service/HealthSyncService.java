package app.talia.sync.service;

import app.talia.activity.model.WorkoutCategory;
import app.talia.activity.repository.DailyStepsRepository;
import app.talia.activity.repository.WorkoutRepository;
import app.talia.config.TaliaProperties;
import app.talia.measurement.repository.WeightEntryRepository;
import app.talia.sync.entity.SyncLogEntity;
import app.talia.sync.model.HealthSyncRequest;
import app.talia.sync.model.HealthSyncResponse;
import app.talia.sync.model.InvalidSyncDataException;
import app.talia.sync.repository.SyncLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Idempotent upserts of Apple Health data (see the {@code upsertFromHealth} repository methods): a row is only
 * touched when a value actually changed, and each upsert tells whether it inserted, updated or found it unchanged.
 */
@Service
public class HealthSyncService {

    private final DailyStepsRepository dailySteps;
    private final WeightEntryRepository weights;
    private final WorkoutRepository workouts;
    private final SyncLogRepository syncLog;
    private final ZoneId zone;

    HealthSyncService(DailyStepsRepository dailySteps, WeightEntryRepository weights, WorkoutRepository workouts,
                      SyncLogRepository syncLog, TaliaProperties properties) {
        this.dailySteps = dailySteps;
        this.weights = weights;
        this.workouts = workouts;
        this.syncLog = syncLog;
        this.zone = properties.timezone();
    }

    @Transactional
    public HealthSyncResponse sync(long userId, HealthSyncRequest request) {
        Tally steps = new Tally();
        for (var s : request.steps()) {
            steps.add(dailySteps.upsertFromHealth(userId, s.date(), s.value()));
        }

        Tally weight = new Tally();
        for (var w : request.weight()) {
            weight.add(weights.upsertFromHealth(userId, FlexibleDateTime.parse(w.ts(), zone), w.value()));
        }

        Tally workoutTally = new Tally();
        for (var w : request.workouts()) {
            LocalDateTime start = FlexibleDateTime.parse(w.start(), zone);
            LocalDateTime end = FlexibleDateTime.parse(w.end(), zone);
            if (end.isBefore(start)) {
                throw new InvalidSyncDataException("Workout ends before it starts: " + w.start());
            }
            String type = w.type().trim();
            workoutTally.add(workouts.upsertFromHealth(userId, start, end, type,
                    WorkoutCategory.fromHealthType(type).name(), w.kcal()));
        }

        syncLog.save(new SyncLogEntity(userId, steps.inserted + weight.inserted + workoutTally.inserted,
                steps.updated + weight.updated + workoutTally.updated));

        return new HealthSyncResponse(steps.toCounts(), weight.toCounts(), workoutTally.toCounts());
    }

    private static final class Tally {
        int inserted;
        int updated;
        int unchanged;

        void add(Optional<Boolean> result) {
            if (result.isEmpty()) {
                unchanged++;
            } else if (result.get()) {
                inserted++;
            } else {
                updated++;
            }
        }

        HealthSyncResponse.Counts toCounts() {
            return new HealthSyncResponse.Counts(inserted, updated, unchanged);
        }
    }
}
