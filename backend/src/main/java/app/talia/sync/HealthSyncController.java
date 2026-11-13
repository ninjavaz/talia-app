package app.talia.sync;

import app.talia.auth.SyncPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthSyncController {

    private final HealthSyncService service;

    HealthSyncController(HealthSyncService service) {
        this.service = service;
    }

    @PostMapping("/api/health-sync")
    HealthSyncResponse sync(@AuthenticationPrincipal SyncPrincipal principal,
                            @Valid @RequestBody HealthSyncRequest request) {
        return service.sync(principal.userId(), request);
    }
}
