package app.talia.measurement.service;

import app.talia.measurement.entity.WeightEntryEntity;
import app.talia.measurement.repository.WeightEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Body weight of a user on a given day, for other features (e.g. the burned-calorie estimate, REQ-022). */
@Service
@Transactional(readOnly = true)
public class WeightService {

    private final WeightEntryRepository weights;

    WeightService(WeightEntryRepository weights) {
        this.weights = weights;
    }

    /**
     * The latest weight entry (manual or synced) on or before {@code day}; if there is none, the earliest entry after
     * it; empty without any weight.
     */
    public Optional<BigDecimal> weightOn(long userId, LocalDate day) {
        return weights.findFirstByUserIdAndMeasuredAtLessThanOrderByMeasuredAtDesc(userId, day.plusDays(1).atStartOfDay())
                .or(() -> weights.findFirstByUserIdOrderByMeasuredAtAsc(userId))
                .map(WeightEntryEntity::getValueKg);
    }
}
