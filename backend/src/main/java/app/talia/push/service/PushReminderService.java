package app.talia.push.service;

import app.talia.common.Weeks;
import app.talia.habit.model.HabitDay;
import app.talia.habit.model.HabitKind;
import app.talia.habit.model.HabitStats;
import app.talia.habit.service.HabitService;
import app.talia.measurement.service.CheckinService;
import app.talia.push.model.PushMessage;
import app.talia.push.model.ReminderKind;
import app.talia.push.repository.PushSentRepository;
import app.talia.settings.model.AppSettings;
import app.talia.settings.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reminder rules (REQ-030, REQ-031, REQ-034) and the test notification (REQ-033). {@link #sendDue()} runs every
 * minute: a reminder is due when its time lies within the last {@link #WINDOW} (so a short downtime still sends it,
 * a long one does not flood), and it is claimed in {@code push_sent} before sending, so it goes out once.
 */
@Service
public class PushReminderService {

    private static final Logger log = LoggerFactory.getLogger(PushReminderService.class);
    static final Duration WINDOW = Duration.ofMinutes(30);

    private final PushService push;
    private final PushSentRepository sent;
    private final SettingsService settings;
    private final HabitService habits;
    private final CheckinService checkins;
    private final Clock clock;

    PushReminderService(PushService push, PushSentRepository sent, SettingsService settings, HabitService habits,
                        CheckinService checkins, Clock clock) {
        this.push = push;
        this.sent = sent;
        this.settings = settings;
        this.habits = habits;
        this.checkins = checkins;
        this.clock = clock;
    }

    /** Sends every reminder that is due now to the users with a device. */
    public void sendDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (long userId : push.usersWithDevices()) {
            try {
                sendDue(userId, now);
            } catch (RuntimeException e) {
                log.warn("Reminders for user {} failed", userId, e);
            }
        }
    }

    /** Removes the de-duplication rows nobody needs any more. */
    public void cleanUp() {
        sent.deleteOlderThan(LocalDate.now(clock).minusDays(30));
    }

    /** REQ-033: a test notification to all devices of the user. */
    public int sendTest(long userId) {
        String lang = settings.get(userId).language();
        return push.sendToUser(userId, new PushMessage(PushTexts.get(lang, "title"), PushTexts.get(lang, "test"),
                "/settings", "test", Duration.ofMinutes(10)));
    }

    private void sendDue(long userId, LocalDateTime now) {
        AppSettings s = settings.get(userId);
        String lang = s.language();
        LocalDate today = now.toLocalDate();

        // REQ-030: each habit with a time, when not done yet
        if (s.pushHabits()) {
            for (HabitDay habit : habits.day(userId, today)) {
                if (habit.remindAt() != null && !habit.done() && isDue(today, habit.remindAt(), now)
                        && claim(userId, ReminderKind.HABIT, habit.id(), today)) {
                    String name = habit.kind() == HabitKind.STEPS
                            ? PushTexts.get(lang, "steps", PushTexts.thousands(lang, s.dailyStepsGoal()))
                            : habit.name();
                    push.sendToUser(userId, new PushMessage(PushTexts.get(lang, "title"),
                            PushTexts.get(lang, "habit", name), "/week", "habit-" + habit.id(), Duration.ofHours(1)));
                }
            }
        }

        // REQ-031: weekly check-in on the configured day, when this week's check-in is missing
        if (s.pushCheckin() && today.getDayOfWeek().getValue() == s.checkinReminderDay()
                && isDue(today, s.checkinReminderTime(), now)
                && !checkins.progress(userId).checkinDoneThisWeek()
                && claim(userId, ReminderKind.CHECKIN, 0, Weeks.start(today))) {
            push.sendToUser(userId, new PushMessage(PushTexts.get(lang, "checkin.title"),
                    PushTexts.get(lang, "checkin.body"), "/progress", "checkin", Duration.ofHours(6)));
        }

        // REQ-034: evening reminder when today is not completed
        if (s.pushStreak() && isDue(today, s.streakReminderTime(), now)) {
            HabitStats stats = habits.stats(userId, today);
            if (stats.total() > 0 && stats.doneToday() < stats.total()
                    && claim(userId, ReminderKind.STREAK, 0, today)) {
                String title = stats.streakDays() > 0
                        ? PushTexts.get(lang, "streak.title", stats.streakDays())
                        : PushTexts.get(lang, "today.title");
                push.sendToUser(userId, new PushMessage(title,
                        PushTexts.get(lang, "streak.body", stats.total() - stats.doneToday(), stats.total()),
                        "/week", "streak", Duration.ofHours(3)));
            }
        }
    }

    /** The reminder time of today lies in (now - WINDOW, now]. */
    private static boolean isDue(LocalDate today, LocalTime time, LocalDateTime now) {
        LocalDateTime at = today.atTime(time);
        return !at.isAfter(now) && at.isAfter(now.minus(WINDOW));
    }

    private boolean claim(long userId, ReminderKind kind, long refId, LocalDate day) {
        return sent.claim(userId, kind.name(), refId, day) == 1;
    }
}
