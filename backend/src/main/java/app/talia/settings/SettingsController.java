package app.talia.settings;

import app.talia.auth.TaliaUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
class SettingsController {

    private final SettingsRepository settings;

    SettingsController(SettingsRepository settings) {
        this.settings = settings;
    }

    @GetMapping
    AppSettings get(@AuthenticationPrincipal TaliaUser user) {
        return settings.get(user.id());
    }

    @PutMapping
    AppSettings update(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody AppSettings body) {
        return settings.update(user.id(), body);
    }
}
