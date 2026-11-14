package app.talia.habit.service;

import app.talia.common.BadRequestException;
import app.talia.common.NotFoundException;
import app.talia.common.Weeks;
import app.talia.habit.entity.HabitEntity;
import app.talia.habit.model.Habit;
import app.talia.habit.model.HabitDay;
import app.talia.habit.model.HabitStats;
import app.talia.habit.repository.HabitCheckRepository;
import app.talia.habit.repository.HabitRepository;
import app.talia.settings.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Daily habits of one user. Deleting a habit only deactivates it, so past checks stay in the history.
 * A week "counts" for the streak when at least {@code habit_week_threshold_pct} of the possible checks were done.
 * Every lookup is scoped by the user - a habit id of another user behaves exactly like a non-existing one.
 */
@Service
@Transactional
public class HabitService {

    private static final int MAX_STREAK_WEEKS = 104;

    private final HabitRepository habits;
    private final HabitCheckRepository checks;
    private final SettingsService settings;
    private final Clock clock;

    HabitService(HabitRepository habits, HabitCheckRepository checks, SettingsService settings, Clock clock) {
        this.habits = habits;
        this.checks = checks;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Habit> list(long userId) {
        return habits.findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(userId).stream()
                .map(HabitService::toModel)
                .toList();
    }

    public Habit create(long userId, String name) {
        int sortOrder = habits.findMaxSortOrder(userId) + 1;
        return toModel(habits.save(new HabitEntity(userId, name.trim(), sortOrder)));
    }

    public Habit rename(long userId, long id, String name) {
        HabitEntity habit = find(userId, id);
        habit.setName(name.trim());
        return toModel(habit);
    }

    public void delete(long userId, long id) {
        find(userId, id).deactivate();
    }

    @Transactional(readOnly = true)
    public List<HabitDay> day(long userId, LocalDate date) {
        Set<Long> done = new HashSet<>(checks.findDoneHabitIds(userId, date));
        return habits.findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(userId).stream()
                .map(h -> new HabitDay(h.getId(), h.getName(), done.contains(h.getId())))
                .toList();
    }

    public void setDone(long userId, long id, LocalDate date, boolean done) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date is in the future");
        }
        if (!habits.existsByIdAndUserIdAndActiveTrue(id, userId)) {
            throw new NotFoundException("Habit not found");
        }
        if (done) {
            checks.check(id, date);
        } else {
            checks.uncheck(id, date);
        }
    }

    @Transactional(readOnly = true)
    public HabitStats stats(long userId, LocalDate date) {
        int threshold = settings.get(userId).habitWeekThresholdPct();
        int total = habits.countByUserIdAndActiveTrue(userId);
        int doneToday = checks.findDoneHabitIds(userId, date).size();
        // checks per week (Monday), active habits only, up to the given date
        Map<LocalDate, Integer> checksPerWeek = checks.countPerWeek(userId, date).stream()
                .collect(Collectors.toMap(HabitCheckRepository.WeekCount::getWeekStart, w -> w.getChecks().intValue()));

        LocalDate thisWeek = Weeks.start(date);
        int daysSoFar = (int) ChronoUnit.DAYS.between(thisWeek, date) + 1;
        int weekPct = percent(checksPerWeek.getOrDefault(thisWeek, 0), total * daysSoFar);

        // The running week counts once it already meets the threshold; if not (yet), it doesn't break the streak.
        int streak = weekPct >= threshold && total > 0 ? 1 : 0;
        LocalDate week = thisWeek.minusWeeks(1);
        while (streak < MAX_STREAK_WEEKS && total > 0
                && percent(checksPerWeek.getOrDefault(week, 0), total * 7) >= threshold) {
            streak++;
            week = week.minusWeeks(1);
        }
        return new HabitStats(doneToday, total, weekPct, threshold, streak);
    }

    private static int percent(int done, int possible) {
        return possible <= 0 ? 0 : Math.min(100, Math.round(done * 100f / possible));
    }

    private HabitEntity find(long userId, long id) {
        return habits.findByIdAndUserIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));
    }

    private static Habit toModel(HabitEntity e) {
        return new Habit(e.getId(), e.getName(), e.getSortOrder());
    }
}
