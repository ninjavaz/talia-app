package app.talia.habit.service;

import app.talia.common.BadRequestException;
import app.talia.common.ConflictException;
import app.talia.common.NotFoundException;
import app.talia.habit.entity.HabitCheckEntity;
import app.talia.habit.entity.HabitEntity;
import app.talia.habit.model.Habit;
import app.talia.habit.model.HabitDay;
import app.talia.habit.model.HabitKind;
import app.talia.habit.model.HabitRequest;
import app.talia.habit.model.HabitStats;
import app.talia.habit.repository.HabitCheckRepository;
import app.talia.habit.repository.HabitRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Daily habits of one user (REQ-010..015). Deleting a habit only deactivates it (from that day on), so past checks
 * and past streak days stay. The streak rule itself lives in {@link StreakRule}.
 * Every lookup is scoped by the user - a habit id of another user behaves exactly like a non-existing one.
 */
@Service
@Transactional
public class HabitService {

    /** Stored name of the steps habit; the UI shows "Kroki: <goal>" instead. */
    static final String STEPS_NAME = "steps";

    private final HabitRepository habits;
    private final HabitCheckRepository checks;
    private final Clock clock;

    HabitService(HabitRepository habits, HabitCheckRepository checks, Clock clock) {
        this.habits = habits;
        this.checks = checks;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Habit> list(long userId) {
        return active(userId).stream().map(HabitService::toModel).toList();
    }

    public Habit create(long userId, HabitRequest request) {
        HabitKind kind = request.kind() != null ? request.kind() : HabitKind.NORMAL;
        String name;
        if (kind == HabitKind.STEPS) {
            if (request.name() != null) {
                throw new BadRequestException("The steps habit has no name");
            }
            if (habits.existsByUserIdAndKindAndActiveTrue(userId, HabitKind.STEPS)) {
                throw new ConflictException("There is already a steps habit");
            }
            name = STEPS_NAME;
        } else {
            name = requireName(request.name());
        }
        int sortOrder = habits.findMaxSortOrder(userId) + 1;
        try {
            return toModel(habits.saveAndFlush(
                    new HabitEntity(userId, name, kind, request.remindAt(), sortOrder, LocalDate.now(clock))));
        } catch (DataIntegrityViolationException e) {
            // two steps habits created at the same moment (unique index)
            throw new ConflictException("There is already a steps habit");
        }
    }

    /** REQ-010: the defaults of a new account, in this order. */
    public void createDefaults(long userId, String language) {
        boolean en = "en".equals(language);
        create(userId, new HabitRequest(en ? "Supplements" : "Suplementy", null, HabitKind.NORMAL));
        create(userId, new HabitRequest(en ? "Drinking water" : "Picie wody", null, HabitKind.NORMAL));
        create(userId, new HabitRequest(null, null, HabitKind.STEPS));
    }

    /** Sets name and time; the steps habit keeps its name (REQ-013). */
    public Habit update(long userId, long id, HabitRequest request) {
        HabitEntity habit = find(userId, id);
        if (habit.getKind() == HabitKind.STEPS) {
            if (request.name() != null) {
                throw new BadRequestException("The steps habit cannot be renamed");
            }
        } else {
            habit.setName(requireName(request.name()));
        }
        habit.setRemindAt(request.remindAt());
        return toModel(habit);
    }

    /** REQ-010: {@code ids} must be exactly the user's active habits, each once. */
    public void reorder(long userId, List<Long> ids) {
        List<HabitEntity> current = active(userId);
        Map<Long, HabitEntity> byId = current.stream().collect(Collectors.toMap(HabitEntity::getId, h -> h));
        if (ids.size() != current.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new BadRequestException("The order must contain each habit exactly once");
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setSortOrder(i + 1);
        }
    }

    public void delete(long userId, long id) {
        find(userId, id).deactivate(LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public List<HabitDay> day(long userId, LocalDate date) {
        Set<Long> done = new HashSet<>(checks.findDoneHabitIds(userId, date));
        return active(userId).stream()
                .map(h -> new HabitDay(h.getId(), h.getName(), h.getKind(), h.getRemindAt(), done.contains(h.getId())))
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

    /** Today's progress (REQ-014) and the streak (REQ-015) as of {@code date}. */
    @Transactional(readOnly = true)
    public HabitStats stats(long userId, LocalDate date) {
        int total = habits.countByUserIdAndActiveTrue(userId);
        int doneToday = checks.findDoneHabitIds(userId, date).size();

        List<HabitEntity> all = habits.findByUserId(userId);
        Map<LocalDate, Set<Long>> donePerDay = checks.findChecks(userId, date.minusDays(StreakRule.MAX_DAYS), date)
                .stream()
                .collect(Collectors.groupingBy(HabitCheckEntity::getDay,
                        Collectors.mapping(HabitCheckEntity::getHabitId, Collectors.toSet())));
        int streak = StreakRule.streak(date, day -> StreakRule.isCompleted(
                all.stream().filter(h -> h.isActiveOn(day)).map(HabitEntity::getId).collect(Collectors.toSet()),
                donePerDay.getOrDefault(day, Set.of())));
        return new HabitStats(doneToday, total, streak);
    }

    private List<HabitEntity> active(long userId) {
        return habits.findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(userId);
    }

    private HabitEntity find(long userId, long id) {
        return habits.findByIdAndUserIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("A habit needs a name");
        }
        return name.trim();
    }

    private static Habit toModel(HabitEntity e) {
        return new Habit(e.getId(), e.getName(), e.getKind(), e.getRemindAt(), e.getSortOrder());
    }
}
