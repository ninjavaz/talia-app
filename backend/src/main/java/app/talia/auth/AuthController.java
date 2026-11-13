package app.talia.auth;

import app.talia.common.BadRequestException;
import app.talia.config.TaliaProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

/**
 * Login, registration (with proof-of-work bot check), session info and personal sync tokens.
 * Throttling: failed logins per IP and per username, registrations and challenges per IP.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 200) String password) {
    }

    /** {@code website} is a honeypot: invisible to people, bots that fill every field give themselves away. */
    record RegisterRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 200) String password,
                           @NotNull @Valid ProofOfWork.Solution proof, String website) {
    }

    record MeResponse(String username) {
    }

    record SyncTokenStatus(boolean configured) {
    }

    record SyncTokenResponse(String token) {
    }

    static class TooManyRequestsException extends RuntimeException {
        final long retryAfterSeconds;

        TooManyRequestsException(long retryAfterSeconds) {
            super("Too many attempts");
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CookieSerializer cookieSerializer;
    private final UserAccounts accounts;
    private final ProofOfWork proofOfWork;
    private final RateLimiter loginFailuresPerIp;
    private final RateLimiter loginFailuresPerUser;
    private final RateLimiter registrationsPerIp;
    private final RateLimiter challengesPerIp;

    AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
                   CookieSerializer cookieSerializer, UserAccounts accounts, ProofOfWork proofOfWork,
                   TaliaProperties properties, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.cookieSerializer = cookieSerializer;
        this.accounts = accounts;
        this.proofOfWork = proofOfWork;
        var s = properties.security();
        this.loginFailuresPerIp = new RateLimiter(s.loginMaxFailuresPerIp(), s.loginWindow(), clock);
        this.loginFailuresPerUser = new RateLimiter(s.loginMaxFailuresPerUser(), s.loginWindow(), clock);
        this.registrationsPerIp = new RateLimiter(s.registrationsPerIp(), s.registrationWindow(), clock);
        this.challengesPerIp = new RateLimiter(s.challengesPerIp(), s.registrationWindow(), clock);
    }

    @PostMapping("/login")
    MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        String ipKey = "ip:" + request.getRemoteAddr();
        String userKey = "user:" + UserAccounts.normalize(body.username());
        if (loginFailuresPerIp.isBlocked(ipKey)) {
            throw new TooManyRequestsException(loginFailuresPerIp.retryAfterSeconds(ipKey));
        }
        if (loginFailuresPerUser.isBlocked(userKey)) {
            throw new TooManyRequestsException(loginFailuresPerUser.retryAfterSeconds(userKey));
        }
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    UserAccounts.normalize(body.username()), body.password()));
        } catch (AuthenticationException e) {
            loginFailuresPerIp.tryAcquire(ipKey);
            loginFailuresPerUser.tryAcquire(userKey);
            throw e;
        }
        loginFailuresPerUser.reset(userKey);
        startSession(auth, request, response);
        return new MeResponse(auth.getName());
    }

    /** A random challenge the browser must solve before registering. */
    @GetMapping("/challenge")
    ProofOfWork.Challenge challenge(HttpServletRequest request) {
        String ipKey = "ip:" + request.getRemoteAddr();
        if (!challengesPerIp.tryAcquire(ipKey)) {
            throw new TooManyRequestsException(challengesPerIp.retryAfterSeconds(ipKey));
        }
        return proofOfWork.create();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    MeResponse register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request,
                        HttpServletResponse response) {
        String ipKey = "ip:" + request.getRemoteAddr();
        if (!registrationsPerIp.tryAcquire(ipKey)) {
            throw new TooManyRequestsException(registrationsPerIp.retryAfterSeconds(ipKey));
        }
        if (body.website() != null && !body.website().isBlank()) {
            throw new BadRequestException("Registration rejected");
        }
        if (!proofOfWork.verify(body.proof())) {
            throw new BadRequestException("Bot check failed, please try again");
        }
        TaliaUser user = accounts.create(body.username(), body.password());
        startSession(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()),
                request, response);
        return new MeResponse(user.getUsername());
    }

    /**
     * Current user. The app calls this on every start, which also re-issues the session cookie with a fresh
     * max-age, so someone who opens the app at least once a year never gets logged out.
     */
    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal TaliaUser user, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, session.getId()));
        }
        return new MeResponse(user.getUsername());
    }

    @GetMapping("/sync-token")
    SyncTokenStatus syncTokenStatus(@AuthenticationPrincipal TaliaUser user) {
        return new SyncTokenStatus(accounts.hasSyncToken(user.id()));
    }

    /** Creates (or replaces) the personal Apple Health sync token. It is returned only this once. */
    @PostMapping("/sync-token")
    SyncTokenResponse newSyncToken(@AuthenticationPrincipal TaliaUser user) {
        return new SyncTokenResponse(accounts.newSyncToken(user.id()));
    }

    private void startSession(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        // Session fixation protection: never keep a session id from before the login.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail badCredentials() {
        // same message for unknown user and wrong password
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemDetail> tooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try later"));
    }
}
