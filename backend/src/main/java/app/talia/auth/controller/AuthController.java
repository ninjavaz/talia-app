package app.talia.auth.controller;

import app.talia.auth.model.Account;
import app.talia.auth.model.EmailNotVerifiedException;
import app.talia.auth.model.ForgotPasswordRequest;
import app.talia.auth.model.LoginRequest;
import app.talia.auth.model.MeResponse;
import app.talia.auth.model.RateLimit;
import app.talia.auth.model.RegisterRequest;
import app.talia.auth.model.ResetPasswordRequest;
import app.talia.auth.model.SyncTokenResponse;
import app.talia.auth.model.SyncTokenStatus;
import app.talia.auth.model.TaliaUser;
import app.talia.auth.model.TokenRequest;
import app.talia.auth.model.TooManyRequestsException;
import app.talia.auth.service.AuthService;
import app.talia.auth.service.BotCheckService;
import app.talia.auth.service.RateLimitService;
import app.talia.auth.service.SyncTokenService;
import app.talia.auth.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

/** Login, registration, e-mail links, session info and personal sync tokens. The logic lives in {@link AuthService}. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService auth;
    private final UserAccountService accounts;
    private final SyncTokenService syncTokens;
    private final BotCheckService botCheck;
    private final RateLimitService limits;
    private final SecurityContextRepository securityContextRepository;
    private final CookieSerializer cookieSerializer;

    AuthController(AuthService auth, UserAccountService accounts, SyncTokenService syncTokens,
                   BotCheckService botCheck, RateLimitService limits,
                   SecurityContextRepository securityContextRepository, CookieSerializer cookieSerializer) {
        this.auth = auth;
        this.accounts = accounts;
        this.syncTokens = syncTokens;
        this.botCheck = botCheck;
        this.limits = limits;
        this.securityContextRepository = securityContextRepository;
        this.cookieSerializer = cookieSerializer;
    }

    @PostMapping("/login")
    MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = auth.login(body.username(), body.password(), request.getRemoteAddr());
        startSession(authentication, request, response);
        return me(((TaliaUser) authentication.getPrincipal()).id());
    }

    /** A signed ALTCHA challenge; the widget solves it before registration. */
    @GetMapping(value = "/challenge", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> challenge(HttpServletRequest request) {
        limits.acquire(RateLimit.CHALLENGE_IP, request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(botCheck.createChallenge());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        auth.register(body, request.getRemoteAddr());
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody TokenRequest body) {
        auth.verifyEmail(body.token());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest request) {
        auth.forgotPassword(body.email(), request.getRemoteAddr());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        auth.resetPassword(body.token(), body.password());
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
        return me(user.id());
    }

    @GetMapping("/sync-token")
    SyncTokenStatus syncTokenStatus(@AuthenticationPrincipal TaliaUser user) {
        return new SyncTokenStatus(syncTokens.hasToken(user.id()));
    }

    /** Creates (or replaces) the personal Apple Health sync token. It is returned only this once. */
    @PostMapping("/sync-token")
    SyncTokenResponse newSyncToken(@AuthenticationPrincipal TaliaUser user) {
        return new SyncTokenResponse(syncTokens.newToken(user.id()));
    }

    private MeResponse me(long userId) {
        Account account = accounts.account(userId);
        return new MeResponse(account.username(), account.email());
    }

    private void startSession(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        // Session fixation protection: never keep a session id from before the login.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail badCredentials() {
        // same message for unknown user and wrong password
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail emailNotVerified() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Confirm your e-mail address first, we sent you a link");
        problem.setProperty("code", "EMAIL_NOT_VERIFIED");
        return problem;
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemDetail> tooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try later"));
    }
}
