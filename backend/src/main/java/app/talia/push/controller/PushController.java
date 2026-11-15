package app.talia.push.controller;

import app.talia.auth.model.TaliaUser;
import app.talia.push.model.PushEndpointRequest;
import app.talia.push.model.PushKey;
import app.talia.push.model.PushSubscriptionRequest;
import app.talia.push.model.PushTestResult;
import app.talia.push.service.PushReminderService;
import app.talia.push.service.PushService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Web Push on the user's devices (REQ-033). */
@RestController
@RequestMapping("/api/push")
class PushController {

    private final PushService push;
    private final PushReminderService reminders;

    PushController(PushService push, PushReminderService reminders) {
        this.push = push;
        this.reminders = reminders;
    }

    @GetMapping("/key")
    PushKey key() {
        return push.key();
    }

    @PutMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void subscribe(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody PushSubscriptionRequest body) {
        push.subscribe(user.id(), body);
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsubscribe(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody PushEndpointRequest body) {
        push.unsubscribe(user.id(), body.endpoint());
    }

    @PostMapping("/test")
    PushTestResult test(@AuthenticationPrincipal TaliaUser user) {
        return new PushTestResult(reminders.sendTest(user.id()));
    }
}
