package app.talia.push.service;

import app.talia.push.model.PushMessage;
import app.talia.settings.service.SettingsService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Reminder rules (REQ-030, REQ-031, REQ-034) and the test notification (REQ-033). */
@Service
public class PushReminderService {

    private final PushService push;
    private final SettingsService settings;

    PushReminderService(PushService push, SettingsService settings) {
        this.push = push;
        this.settings = settings;
    }

    /** REQ-033: a test notification to all devices of the user. */
    public int sendTest(long userId) {
        String lang = settings.get(userId).language();
        return push.sendToUser(userId, new PushMessage(PushTexts.get(lang, "title"), PushTexts.get(lang, "test"),
                "/settings", "test", Duration.ofMinutes(10)));
    }
}
