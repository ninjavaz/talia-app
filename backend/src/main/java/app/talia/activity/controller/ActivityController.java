package app.talia.activity.controller;

import app.talia.activity.model.WeekView;
import app.talia.activity.model.Workout;
import app.talia.activity.model.WorkoutRequest;
import app.talia.activity.service.WeekService;
import app.talia.activity.service.WorkoutService;
import app.talia.auth.model.TaliaUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
class ActivityController {

    private final WorkoutService workouts;
    private final WeekService weeks;

    ActivityController(WorkoutService workouts, WeekService weeks) {
        this.workouts = workouts;
        this.weeks = weeks;
    }

    /** Everything for the "Week" screen. {@code date} picks the week (default: current week). */
    @GetMapping("/api/week")
    WeekView week(@AuthenticationPrincipal TaliaUser user,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return weeks.week(user.id(), date);
    }

    @GetMapping("/api/workouts")
    List<Workout> list(@AuthenticationPrincipal TaliaUser user,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return workouts.between(user.id(), from, to);
    }

    @PostMapping("/api/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    Workout add(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody WorkoutRequest body) {
        return workouts.add(user.id(), body.date(), body.category(), body.durationMin());
    }

    @DeleteMapping("/api/workouts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal TaliaUser user, @PathVariable long id) {
        workouts.delete(user.id(), id);
    }
}
