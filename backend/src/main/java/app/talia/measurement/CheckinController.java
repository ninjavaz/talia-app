package app.talia.measurement;

import app.talia.auth.TaliaUser;
import app.talia.measurement.CheckinDtos.CheckinRequest;
import app.talia.measurement.CheckinDtos.ProgressView;
import app.talia.measurement.CheckinDtos.WeekEntry;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
class CheckinController {

    private final CheckinService service;

    CheckinController(CheckinService service) {
        this.service = service;
    }

    @GetMapping("/api/progress")
    ProgressView progress(@AuthenticationPrincipal TaliaUser user) {
        return service.progress(user.id());
    }

    @GetMapping("/api/checkins")
    List<WeekEntry> history(@AuthenticationPrincipal TaliaUser user, @RequestParam(defaultValue = "26") int limit) {
        return service.history(user.id(), Math.clamp(limit, 1, 520));
    }

    /** Creates or replaces the check-in of the week containing {@code date}. */
    @PutMapping("/api/checkins")
    WeekEntry save(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody CheckinRequest request) {
        return service.save(user.id(), request);
    }

    @DeleteMapping("/api/checkins/{weekDay}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal TaliaUser user,
                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekDay) {
        service.deleteWeek(user.id(), weekDay);
    }
}
