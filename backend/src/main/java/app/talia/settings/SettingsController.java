package app.talia.settings;

import jakarta.validation.Valid;
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
    AppSettings get() {
        return settings.get();
    }

    @PutMapping
    AppSettings update(@Valid @RequestBody AppSettings body) {
        return settings.update(body);
    }
}
