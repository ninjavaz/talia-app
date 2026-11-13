package app.talia.measurement;

import app.talia.auth.TaliaUser;
import app.talia.measurement.BodyPartService.BodyPart;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/body-parts")
class BodyPartController {

    record BodyPartRequest(@NotBlank @Size(max = 50) String name) {
    }

    private final BodyPartService service;

    BodyPartController(BodyPartService service) {
        this.service = service;
    }

    @GetMapping
    List<BodyPart> list(@AuthenticationPrincipal TaliaUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BodyPart create(@AuthenticationPrincipal TaliaUser user, @Valid @RequestBody BodyPartRequest body) {
        return service.create(user.id(), body.name());
    }

    @PutMapping("/{id}")
    BodyPart rename(@AuthenticationPrincipal TaliaUser user, @PathVariable long id,
                    @Valid @RequestBody BodyPartRequest body) {
        return service.rename(user.id(), id, body.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal TaliaUser user, @PathVariable long id) {
        service.delete(user.id(), id);
    }
}
