package app.talia.habit;

import app.talia.auth.TaliaUser;
import app.talia.habit.HabitService.Habit;
import app.talia.habit.HabitService.HabitDay;
import app.talia.habit.HabitService.HabitStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
class HabitController {

    record HabitRequest(@NotBlank @Size(max = 100) String name) {
    }

    private final HabitService service;
    private final Clock clock;

    HabitController(HabitService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    List<Habit> list(@AuthenticationPrincipal TaliaUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Habit create(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody HabitRequest body) {
        return service.create(user.id(), body.name());
    }

    @PutMapping("/{id}")
    Habit rename(@AuthenticationPrincipal TaliaUser user, @PathVariable long id, @Valid @RequestBody HabitRequest body) {
        return service.rename(user.id(), id, body.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal TaliaUser user, @PathVariable long id) {
        service.delete(user.id(), id);
    }

    /** Habits with their done flag for a day (default: today). */
    @GetMapping("/day")
    List<HabitDay> day(@AuthenticationPrincipal TaliaUser user,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.day(user.id(), date != null ? date : LocalDate.now(clock));
    }

    @PutMapping("/{id}/checks/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void check(@AuthenticationPrincipal TaliaUser user, @PathVariable long id,
               @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.setDone(user.id(), id, date, true);
    }

    @DeleteMapping("/{id}/checks/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void uncheck(@AuthenticationPrincipal TaliaUser user, @PathVariable long id,
                 @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.setDone(user.id(), id, date, false);
    }

    @GetMapping("/stats")
    HabitStats stats(@AuthenticationPrincipal TaliaUser user,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.stats(user.id(), date != null ? date : LocalDate.now(clock));
    }
}
