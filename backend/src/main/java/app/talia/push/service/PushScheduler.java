package app.talia.push.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the reminders every minute in the app's time zone. Off in tests ({@code talia.push.reminders-enabled=false}),
 * which call {@link PushReminderService#sendDue()} with a fixed clock instead.
 */
@Component
@ConditionalOnProperty(name = "talia.push.reminders-enabled", havingValue = "true", matchIfMissing = true)
class PushScheduler {

    private final PushReminderService reminders;

    PushScheduler(PushReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(cron = "0 * * * * *", zone = "${talia.timezone}")
    void sendDue() {
        reminders.sendDue();
    }

    @Scheduled(cron = "0 17 3 * * *", zone = "${talia.timezone}")
    void cleanUp() {
        reminders.cleanUp();
    }
}
