package app.talia.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record MeResponse(String username) {
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

        // Session fixation protection: never keep a session id from before the login.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return new MeResponse(auth.getName());
    }

    @GetMapping("/me")
    MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail badCredentials() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
}
