package app.talia.sync.controller;

import app.talia.auth.model.SyncPrincipal;
import app.talia.sync.model.HealthSyncRequest;
import app.talia.sync.model.HealthSyncResponse;
import app.talia.sync.service.HealthSyncService;
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
