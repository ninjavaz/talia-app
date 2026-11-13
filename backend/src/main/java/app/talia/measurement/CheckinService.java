package app.talia.measurement;

import app.talia.activity.StepsQueries;
import app.talia.common.BadRequestException;
import app.talia.common.Weeks;
import app.talia.measurement.BodyPartService.BodyPart;
import app.talia.measurement.CheckinDtos.BodyPartProgress;
import app.talia.measurement.CheckinDtos.CheckinRequest;
import app.talia.measurement.CheckinDtos.Metric;
import app.talia.measurement.CheckinDtos.PartValue;
import app.talia.measurement.CheckinDtos.Point;
import app.talia.measurement.CheckinDtos.ProgressView;
import app.talia.measurement.CheckinDtos.WeekEntry;
import app.talia.settings.SettingsRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Weekly check-in (body measurements, weight, average daily steps) and the progress overview.
 * There is one manual check-in per week; saving again replaces it.
 */
@Service
class CheckinService {

    /** Manual weight entries are stored at this time of day (synced ones keep their real timestamp). */
    private static final LocalTime MANUAL_WEIGHT_TIME = LocalTime.of(8, 0);

    private final JdbcClient jdbc;
    private final Clock clock;
    private final StepsQueries steps;
    private final SettingsRepository settings;
    private final BodyPartService bodyParts;

    CheckinService(JdbcClient jdbc, Clock clock, StepsQueries steps, SettingsRepository settings,
                   BodyPartService bodyParts) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.steps = steps;
        this.settings = settings;
        this.bodyParts = bodyParts;
    }

    @Transactional
    WeekEntry save(CheckinRequest request) {
        LocalDate date = request.date();
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date is in the future");
        }
        var seen = new HashSet<Long>();
        for (PartValue m : request.measurements()) {
            if (!seen.add(m.partId()) || !bodyParts.isActive(m.partId())) {
                throw new BadRequestException("Unknown or duplicate body part " + m.partId());
            }
        }

        LocalDate start = Weeks.start(date);
        deleteManualData(start);

        for (PartValue m : request.measurements()) {
            jdbc.sql("INSERT INTO body_measurement (body_part_id, measured_on, value_cm) VALUES (:part, :date, :value)")
                    .param("part", m.partId()).param("date", date).param("value", m.valueCm()).update();
        }
        if (request.weightKg() != null) {
            jdbc.sql("""
                            INSERT INTO weight_entry (measured_at, value_kg, source) VALUES (:ts, :value, 'MANUAL')
                            ON CONFLICT (measured_at) DO UPDATE
                               SET value_kg = EXCLUDED.value_kg, source = 'MANUAL', updated_at = now()
                            """)
                    .param("ts", date.atTime(MANUAL_WEIGHT_TIME)).param("value", request.weightKg()).update();
        }
        if (request.avgDailySteps() != null) {
            jdbc.sql("INSERT INTO weekly_steps (week_start, avg_daily_steps) VALUES (:start, :steps)")
                    .param("start", start).param("steps", request.avgDailySteps()).update();
        }

        return history(Integer.MAX_VALUE).stream()
                .filter(w -> w.weekStart().equals(start))
                .findFirst()
                .orElseThrow();
    }

    /** Removes the manual check-in of a week (synced Health data and hidden body parts stay). */
    @Transactional
    void deleteWeek(LocalDate anyDay) {
        deleteManualData(Weeks.start(anyDay));
    }

    private void deleteManualData(LocalDate start) {
        LocalDate end = start.plusDays(6);
        jdbc.sql("""
                        DELETE FROM body_measurement
                        WHERE measured_on BETWEEN :start AND :end
                          AND body_part_id IN (SELECT id FROM body_part WHERE active)
                        """)
                .param("start", start).param("end", end).update();
        jdbc.sql("DELETE FROM weight_entry WHERE source = 'MANUAL' AND measured_at >= :start AND measured_at < :next")
                .param("start", start.atStartOfDay()).param("next", end.plusDays(1).atStartOfDay()).update();
        jdbc.sql("DELETE FROM weekly_steps WHERE week_start = :start").param("start", start).update();
    }

    List<WeekEntry> history(int limit) {
        record WeekRow(LocalDate weekStart, BigDecimal weightKg, Integer steps) {
        }
        List<WeekRow> weeks = jdbc.sql("""
                        WITH weeks AS (
                            SELECT date_trunc('week', m.measured_on)::date AS wk
                            FROM body_measurement m JOIN body_part p ON p.id = m.body_part_id AND p.active
                            UNION SELECT date_trunc('week', measured_at)::date FROM weight_entry
                            UNION SELECT week_start FROM weekly_steps
                        ), weight AS (
                            SELECT date_trunc('week', measured_at)::date AS wk, round(avg(value_kg), 1) AS value_kg
                            FROM weight_entry GROUP BY 1
                        )
                        SELECT w.wk, weight.value_kg, s.avg_daily_steps
                        FROM weeks w
                        LEFT JOIN weight ON weight.wk = w.wk
                        LEFT JOIN weekly_steps s ON s.week_start = w.wk
                        ORDER BY w.wk DESC
                        LIMIT :limit
                        """)
                .param("limit", limit)
                .query((rs, n) -> new WeekRow(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2),
                        (Integer) rs.getObject(3)))
                .list();
        if (weeks.isEmpty()) {
            return List.of();
        }

        // latest value per (week, part), active parts only, in the parts' display order
        Map<LocalDate, List<PartValue>> perWeek = new LinkedHashMap<>();
        jdbc.sql("""
                        SELECT DISTINCT ON (date_trunc('week', m.measured_on), m.body_part_id)
                               date_trunc('week', m.measured_on)::date, m.body_part_id, m.value_cm, p.sort_order
                        FROM body_measurement m JOIN body_part p ON p.id = m.body_part_id AND p.active
                        WHERE m.measured_on >= :from
                        ORDER BY date_trunc('week', m.measured_on), m.body_part_id, m.measured_on DESC
                        """)
                .param("from", weeks.getLast().weekStart())
                .query((rs, n) -> new Object[]{rs.getObject(1, LocalDate.class), rs.getLong(2), rs.getBigDecimal(3),
                        rs.getInt(4)})
                .list().stream()
                .sorted((a, b) -> Integer.compare((int) a[3], (int) b[3]))
                .forEach(r -> perWeek.computeIfAbsent((LocalDate) r[0], k -> new ArrayList<>())
                        .add(new PartValue((Long) r[1], (BigDecimal) r[2])));

        return weeks.stream()
                .map(w -> new WeekEntry(w.weekStart(), perWeek.getOrDefault(w.weekStart(), List.of()),
                        w.weightKg(), w.steps()))
                .toList();
    }

    ProgressView progress() {
        LocalDate today = LocalDate.now(clock);

        List<BodyPartProgress> parts = new ArrayList<>();
        for (BodyPart part : bodyParts.list()) {
            List<Point> points = jdbc.sql("""
                            SELECT measured_on, value_cm FROM body_measurement
                            WHERE body_part_id = :part ORDER BY measured_on
                            """)
                    .param("part", part.id())
                    .query((rs, n) -> new Point(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
                    .list();
            parts.add(new BodyPartProgress(part.id(), part.name(), part.code(), metric(points, 1), points));
        }
        Metric waist = parts.stream()
                .filter(p -> BodyPartService.WAIST.equals(p.code()))
                .map(BodyPartProgress::metric)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        List<Point> weightPoints = jdbc.sql("""
                        SELECT measured_at::date, round(avg(value_kg), 2)
                        FROM weight_entry GROUP BY 1 ORDER BY 1
                        """)
                .query((rs, n) -> new Point(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
                .list();
        List<Point> weightTrend = movingAverage(weightPoints, 7);

        LocalDate lastCheckin = jdbc.sql("""
                        SELECT max(d) FROM (
                            SELECT max(m.measured_on) AS d
                            FROM body_measurement m JOIN body_part p ON p.id = m.body_part_id AND p.active
                            UNION ALL SELECT max(measured_at)::date FROM weight_entry WHERE source = 'MANUAL'
                            UNION ALL SELECT max(week_start) + 6 FROM weekly_steps
                        ) t
                        """)
                .query(LocalDate.class).optional().orElse(null);
        LocalDate weekStart = Weeks.start(today);
        boolean doneThisWeek = lastCheckin != null && !lastCheckin.isBefore(weekStart);
        boolean due = !doneThisWeek
                && (today.getDayOfWeek() == DayOfWeek.SUNDAY
                || lastCheckin == null
                || lastCheckin.isBefore(today.minusDays(7)));
        // a steps-only check-in "ends" on Sunday; never report a date in the future
        if (lastCheckin != null && lastCheckin.isAfter(today)) {
            lastCheckin = today;
        }

        return new ProgressView(
                waist,
                metric(weightTrend, 1),
                parts,
                weightPoints,
                weightTrend,
                steps.avgDailySteps(today).orElse(null),
                steps.avgDailySteps(today.minusWeeks(1)).orElse(null),
                settings.get().dailyStepsGoal(),
                lastCheckin,
                doneThisWeek,
                due);
    }

    /** Trailing average over the values whose date lies within the last {@code days} days of each point. */
    static List<Point> movingAverage(List<Point> points, int days) {
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
