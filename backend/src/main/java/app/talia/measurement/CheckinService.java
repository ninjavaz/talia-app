package app.talia.measurement;

import app.talia.activity.StepsQueries;
import app.talia.common.BadRequestException;
import app.talia.common.Weeks;
import app.talia.measurement.CheckinDtos.CheckinRequest;
import app.talia.measurement.CheckinDtos.Metric;
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
import java.util.List;

/**
 * Weekly check-in (waist, weight, average daily steps) and the progress overview.
 * There is at most one manual check-in per week: saving again within the same week replaces it.
 */
@Service
class CheckinService {

    /** Manual weight entries are stored at this time of day (synced ones keep their real timestamp). */
    private static final LocalTime MANUAL_WEIGHT_TIME = LocalTime.of(8, 0);

    private final JdbcClient jdbc;
    private final Clock clock;
    private final StepsQueries steps;
    private final SettingsRepository settings;

    CheckinService(JdbcClient jdbc, Clock clock, StepsQueries steps, SettingsRepository settings) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.steps = steps;
        this.settings = settings;
    }

    @Transactional
    WeekEntry save(CheckinRequest request) {
        LocalDate date = request.date();
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date is in the future");
        }
        LocalDate start = Weeks.start(date);
        LocalDate end = start.plusDays(6);

        if (request.waistCm() != null) {
            jdbc.sql("DELETE FROM waist_entry WHERE measured_on BETWEEN :start AND :end AND measured_on <> :date")
                    .param("start", start).param("end", end).param("date", date).update();
            jdbc.sql("""
                            INSERT INTO waist_entry (measured_on, value_cm) VALUES (:date, :value)
                            ON CONFLICT (measured_on) DO UPDATE SET value_cm = EXCLUDED.value_cm
                            """)
                    .param("date", date).param("value", request.waistCm()).update();
        }

        if (request.weightKg() != null) {
            jdbc.sql("""
                            DELETE FROM weight_entry
                            WHERE source = 'MANUAL' AND measured_at >= :start AND measured_at < :next
                            """)
                    .param("start", start.atStartOfDay()).param("next", end.plusDays(1).atStartOfDay()).update();
            jdbc.sql("""
                            INSERT INTO weight_entry (measured_at, value_kg, source) VALUES (:ts, :value, 'MANUAL')
                            ON CONFLICT (measured_at) DO UPDATE
                               SET value_kg = EXCLUDED.value_kg, source = 'MANUAL', updated_at = now()
                            """)
                    .param("ts", date.atTime(MANUAL_WEIGHT_TIME)).param("value", request.weightKg()).update();
        }

        if (request.avgDailySteps() != null) {
            jdbc.sql("""
                            INSERT INTO weekly_steps (week_start, avg_daily_steps) VALUES (:start, :steps)
                            ON CONFLICT (week_start) DO UPDATE
                               SET avg_daily_steps = EXCLUDED.avg_daily_steps, updated_at = now()
                            """)
                    .param("start", start).param("steps", request.avgDailySteps()).update();
        }

        return history(Integer.MAX_VALUE).stream()
                .filter(w -> w.weekStart().equals(start))
                .findFirst()
                .orElseThrow();
    }

    /** Removes the manual check-in of a week (synced Health data stays). */
    @Transactional
    void deleteWeek(LocalDate anyDay) {
        LocalDate start = Weeks.start(anyDay);
        LocalDate end = start.plusDays(6);
        jdbc.sql("DELETE FROM waist_entry WHERE measured_on BETWEEN :start AND :end")
                .param("start", start).param("end", end).update();
        jdbc.sql("DELETE FROM weight_entry WHERE source = 'MANUAL' AND measured_at >= :start AND measured_at < :next")
                .param("start", start.atStartOfDay()).param("next", end.plusDays(1).atStartOfDay()).update();
        jdbc.sql("DELETE FROM weekly_steps WHERE week_start = :start").param("start", start).update();
    }

    List<WeekEntry> history(int limit) {
        return jdbc.sql("""
                        WITH weeks AS (
                            SELECT date_trunc('week', measured_on)::date AS wk FROM waist_entry
                            UNION SELECT date_trunc('week', measured_at)::date FROM weight_entry
                            UNION SELECT week_start FROM weekly_steps
                        ), waist AS (
                            SELECT DISTINCT ON (date_trunc('week', measured_on))
                                   date_trunc('week', measured_on)::date AS wk, value_cm
                            FROM waist_entry ORDER BY date_trunc('week', measured_on), measured_on DESC
                        ), weight AS (
                            SELECT date_trunc('week', measured_at)::date AS wk, round(avg(value_kg), 1) AS value_kg
                            FROM weight_entry GROUP BY 1
                        )
                        SELECT w.wk, waist.value_cm, weight.value_kg, s.avg_daily_steps
                        FROM weeks w
                        LEFT JOIN waist ON waist.wk = w.wk
                        LEFT JOIN weight ON weight.wk = w.wk
                        LEFT JOIN weekly_steps s ON s.week_start = w.wk
                        ORDER BY w.wk DESC
                        LIMIT :limit
                        """)
                .param("limit", limit)
                .query((rs, n) -> new WeekEntry(
                        rs.getObject(1, LocalDate.class),
                        rs.getBigDecimal(2),
                        rs.getBigDecimal(3),
                        (Integer) rs.getObject(4)))
                .list();
    }

    ProgressView progress() {
        LocalDate today = LocalDate.now(clock);

        List<Point> waistPoints = jdbc.sql("SELECT measured_on, value_cm FROM waist_entry ORDER BY measured_on")
                .query((rs, n) -> new Point(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
                .list();
        List<Point> weightPoints = jdbc.sql("""
                        SELECT measured_at::date, round(avg(value_kg), 2)
                        FROM weight_entry GROUP BY 1 ORDER BY 1
                        """)
                .query((rs, n) -> new Point(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
                .list();
        List<Point> weightTrend = movingAverage(weightPoints, 7);

        LocalDate lastCheckin = waistPoints.isEmpty() ? null : waistPoints.getLast().date();
        boolean doneThisWeek = lastCheckin != null && !lastCheckin.isBefore(Weeks.start(today));
        boolean due = !doneThisWeek
                && (today.getDayOfWeek() == DayOfWeek.SUNDAY
                || lastCheckin == null
                || lastCheckin.isBefore(today.minusDays(7)));

        return new ProgressView(
                metric(waistPoints, 1),
                metric(weightTrend, 1),
                waistPoints,
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
