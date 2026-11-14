package app.talia.measurement.service;

import app.talia.activity.entity.WeeklyStepsEntity;
import app.talia.activity.repository.WeeklyStepsRepository;
import app.talia.activity.service.StepsService;
import app.talia.common.BadRequestException;
import app.talia.common.Weeks;
import app.talia.measurement.entity.BodyMeasurementEntity;
import app.talia.measurement.model.BodyPart;
import app.talia.measurement.model.BodyPartProgress;
import app.talia.measurement.model.CheckinRequest;
import app.talia.measurement.model.Metric;
import app.talia.measurement.model.PartValue;
import app.talia.measurement.model.Point;
import app.talia.measurement.model.ProgressView;
import app.talia.measurement.model.WeekEntry;
import app.talia.measurement.repository.BodyMeasurementRepository;
import app.talia.measurement.repository.WeightEntryRepository;
import app.talia.settings.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Weekly check-in (body measurements, weight, average daily steps) and the progress overview of one user.
 * There is one manual check-in per week; saving again replaces it. Every query is scoped by the user.
 */
@Service
@Transactional
public class CheckinService {

    /** Manual weight entries are stored at this time of day (synced ones keep their real timestamp). */
    private static final LocalTime MANUAL_WEIGHT_TIME = LocalTime.of(8, 0);

    private final BodyMeasurementRepository measurements;
    private final WeightEntryRepository weights;
    private final WeeklyStepsRepository weeklySteps;
    private final BodyPartService bodyParts;
    private final StepsService steps;
    private final SettingsService settings;
    private final Clock clock;

    CheckinService(BodyMeasurementRepository measurements, WeightEntryRepository weights,
                   WeeklyStepsRepository weeklySteps, BodyPartService bodyParts, StepsService steps,
                   SettingsService settings, Clock clock) {
        this.measurements = measurements;
        this.weights = weights;
        this.weeklySteps = weeklySteps;
        this.bodyParts = bodyParts;
        this.steps = steps;
        this.settings = settings;
        this.clock = clock;
    }

    public WeekEntry save(long userId, CheckinRequest request) {
        LocalDate date = request.date();
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date is in the future");
        }
        var seen = new HashSet<Long>();
        for (PartValue m : request.measurements()) {
            // also rejects body parts of other users
            if (!seen.add(m.partId()) || !bodyParts.isActive(userId, m.partId())) {
                throw new BadRequestException("Unknown or duplicate body part " + m.partId());
            }
        }

        LocalDate start = Weeks.start(date);
        deleteManualData(userId, start);

        measurements.saveAll(request.measurements().stream()
                .map(m -> new BodyMeasurementEntity(m.partId(), date, m.valueCm()))
                .toList());
        if (request.weightKg() != null) {
            weights.upsertManual(userId, date.atTime(MANUAL_WEIGHT_TIME), request.weightKg());
        }
        if (request.avgDailySteps() != null) {
            weeklySteps.save(new WeeklyStepsEntity(userId, start, request.avgDailySteps()));
        }

        return history(userId, Integer.MAX_VALUE).stream()
                .filter(w -> w.weekStart().equals(start))
                .findFirst()
                .orElseThrow();
    }

    /** Removes the manual check-in of a week (synced Health data and hidden body parts stay). */
    public void deleteWeek(long userId, LocalDate anyDay) {
        deleteManualData(userId, Weeks.start(anyDay));
    }

    private void deleteManualData(long userId, LocalDate start) {
        LocalDate end = start.plusDays(6);
        measurements.deleteBetween(userId, start, end);
        weights.deleteManualBetween(userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        weeklySteps.deleteWeek(userId, start);
    }

    /** Newest week first: every week with a measurement, a weight or a steps value. */
    @Transactional(readOnly = true)
    public List<WeekEntry> history(long userId, int limit) {
        Map<LocalDate, List<PartValue>> partsPerWeek = measurements.findLatestPerWeek(userId).stream()
                .sorted(Comparator.comparing(BodyMeasurementRepository.WeekValue::getSortOrder)
                        .thenComparing(BodyMeasurementRepository.WeekValue::getPartId))
                .collect(Collectors.groupingBy(BodyMeasurementRepository.WeekValue::getWeekStart,
                        Collectors.mapping(v -> new PartValue(v.getPartId(), v.getValueCm()), Collectors.toList())));
        Map<LocalDate, BigDecimal> weightPerWeek = weights.findWeeklyAverages(userId).stream()
                .collect(Collectors.toMap(WeightEntryRepository.DatedValue::getDate,
                        WeightEntryRepository.DatedValue::getValue));
        Map<LocalDate, Integer> stepsPerWeek = weeklySteps.findByUserId(userId).stream()
                .collect(Collectors.toMap(WeeklyStepsEntity::getWeekStart, WeeklyStepsEntity::getAvgDailySteps));

        TreeSet<LocalDate> weeks = new TreeSet<>(Comparator.reverseOrder());
        weeks.addAll(partsPerWeek.keySet());
        weeks.addAll(weightPerWeek.keySet());
        weeks.addAll(stepsPerWeek.keySet());
        return weeks.stream()
                .limit(limit)
                .map(week -> new WeekEntry(week, partsPerWeek.getOrDefault(week, List.of()), weightPerWeek.get(week),
                        stepsPerWeek.get(week)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressView progress(long userId) {
        LocalDate today = LocalDate.now(clock);

        List<BodyPartProgress> parts = new ArrayList<>();
        for (BodyPart part : bodyParts.list(userId)) {
            // the part belongs to the user (listed above), so its measurements do too
            List<Point> points = measurements.findByBodyPartIdOrderByMeasuredOn(part.id()).stream()
                    .map(m -> new Point(m.getMeasuredOn(), m.getValueCm()))
                    .toList();
            parts.add(new BodyPartProgress(part.id(), part.name(), metric(points, 1), points));
        }

        List<Point> weightPoints = weights.findDailyAverages(userId).stream()
                .map(v -> new Point(v.getDate(), v.getValue()))
                .toList();
        List<Point> weightTrend = movingAverage(weightPoints, 7);

        LocalDate lastCheckin = Stream.of(
                        measurements.findLastDate(userId),
                        weights.findLastManual(userId).map(LocalDateTime::toLocalDate),
                        // a steps-only check-in "ends" on Sunday
                        weeklySteps.findLastWeekStart(userId).map(start -> start.plusDays(6)))
                .flatMap(Optional::stream)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        LocalDate weekStart = Weeks.start(today);
        boolean doneThisWeek = lastCheckin != null && !lastCheckin.isBefore(weekStart);
        boolean due = !doneThisWeek
                && (today.getDayOfWeek() == DayOfWeek.SUNDAY
                || lastCheckin == null
                || lastCheckin.isBefore(today.minusDays(7)));
        // never report a date in the future
        if (lastCheckin != null && lastCheckin.isAfter(today)) {
            lastCheckin = today;
        }

        return new ProgressView(
                metric(weightTrend, 1),
                parts,
                weightPoints,
                weightTrend,
                steps.avgDailySteps(userId, today).orElse(null),
                steps.avgDailySteps(userId, today.minusWeeks(1)).orElse(null),
                settings.get(userId).dailyStepsGoal(),
                lastCheckin,
                doneThisWeek,
                due);
    }

    /** Trailing average over the values whose date lies within the last {@code days} days of each point. */
    public static List<Point> movingAverage(List<Point> points, int days) {
        List<Point> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            LocalDate date = points.get(i).date();
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (int j = i; j >= 0 && points.get(j).date().isAfter(date.minusDays(days)); j--) {
                sum = sum.add(points.get(j).value());
                count++;
            }
            result.add(new Point(date, sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private static Metric metric(List<Point> points, int scale) {
        if (points.isEmpty()) {
            return null;
        }
        Point first = points.getFirst();
        Point latest = points.getLast();
        BigDecimal latestValue = latest.value().setScale(scale, RoundingMode.HALF_UP);
        BigDecimal firstValue = first.value().setScale(scale, RoundingMode.HALF_UP);
        return new Metric(latestValue, latest.date(), firstValue, latestValue.subtract(firstValue));
    }
}
