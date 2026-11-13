package app.talia.auth;

import app.talia.auth.RateLimits.Limit;
import app.talia.auth.UserAccounts.Account;
import app.talia.auth.UserAccounts.TokenPurpose;
import app.talia.common.BadRequestException;
import app.talia.config.TaliaProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/**
 * Login, registration (ALTCHA bot check + e-mail confirmation), password reset by e-mail, session info and
 * personal sync tokens. Rate limits (Bucket4j, see {@link RateLimits}): failed logins per IP and per username,
 * registrations and challenges per IP, e-mails per IP and per address.
 * <p>
 * Answers never reveal whether an e-mail address has an account: registration with a used address and
 * "forgot password" for an unknown address answer exactly like the successful case.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record LoginRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 200) String password) {
    }

    /**
     * @param altcha  base64 payload from the ALTCHA widget (solved proof of work)
     * @param website honeypot: invisible to people, bots that fill every field give themselves away
     */
    record RegisterRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 300) String email,
                           @NotBlank @Size(max = 200) String password, @NotBlank @Size(max = 4000) String altcha,
                           String website, String language) {
    }

    record TokenRequest(@NotBlank @Size(max = 100) String token) {
    }

    record ForgotPasswordRequest(@NotBlank @Size(max = 300) String email) {
    }

    record ResetPasswordRequest(@NotBlank @Size(max = 100) String token, @NotBlank @Size(max = 200) String password) {
    }

    record MeResponse(String username, String email) {
    }

    record SyncTokenStatus(boolean configured) {
    }

    record SyncTokenResponse(String token) {
    }

    /** Right password, but the e-mail is not confirmed yet (a fresh link was sent). */
    static class EmailNotVerifiedException extends RuntimeException {
        EmailNotVerifiedException() {
            super("E-mail not confirmed");
        }
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CookieSerializer cookieSerializer;
    private final UserAccounts accounts;
    private final AccountMails mails;
    private final BotCheck botCheck;
    private final RateLimits limits;
    private final TaliaProperties.Security security;

    AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
                   CookieSerializer cookieSerializer, UserAccounts accounts, AccountMails mails, BotCheck botCheck,
                   RateLimits limits, TaliaProperties properties) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.cookieSerializer = cookieSerializer;
        this.accounts = accounts;
        this.mails = mails;
        this.botCheck = botCheck;
        this.limits = limits;
        this.security = properties.security();
    }

    @PostMapping("/login")
    MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        String username = UserAccounts.normalize(body.username());
        limits.check(Limit.LOGIN_IP, ip);
        limits.check(Limit.LOGIN_USER, username);
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, body.password()));
        } catch (AuthenticationException e) {
            limits.record(Limit.LOGIN_IP, ip);
            limits.record(Limit.LOGIN_USER, username);
            throw e;
        }
        limits.reset(Limit.LOGIN_USER, username);
        Account account = accounts.account(((TaliaUser) auth.getPrincipal()).id());
        if (!account.emailVerified()) {
            // only told after the right password, so this does not reveal anything to strangers
            sendVerificationMail(account, ip);
            throw new EmailNotVerifiedException();
        }
        startSession(auth, request, response);
        return new MeResponse(account.username(), account.email());
    }

    /** A signed ALTCHA challenge; the widget solves it before registration. */
    @GetMapping(value = "/challenge", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> challenge(HttpServletRequest request) {
        limits.acquire(Limit.CHALLENGE_IP, request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(botCheck.createChallenge());
    }

    /**
     * Creates an unconfirmed account and e-mails a confirmation link. Login works after confirming.
     * An address that already has an account gets a notice instead, and the answer is the same.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        limits.acquire(Limit.REGISTER_IP, ip);
        if (body.website() != null && !body.website().isBlank()) {
            throw new BadRequestException("Registration rejected");
        }
        if (!botCheck.verify(body.altcha())) {
            throw new BadRequestException("Bot check failed, please try again");
        }
        String language = "en".equals(body.language()) ? "en" : "pl";
        try {
            TaliaUser user = accounts.create(body.username(), body.email(), body.password(), language);
            sendVerificationMail(accounts.account(user.id()), ip);
        } catch (UserAccounts.EmailTakenException e) {
            accounts.accountByEmail(body.email()).ifPresent(existing -> {
                if (limits.tryAcquire(Limit.MAIL_ADDRESS, existing.email())) {
                    mails.alreadyRegistered(existing.email(), existing.username(), existing.language());
                }
            });
        }
    }

    /** Confirms the e-mail address with the token from the link. */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody TokenRequest body) {
        if (!accounts.verifyEmail(body.token())) {
            throw new BadRequestException("The link is invalid or expired");
        }
    }

    /** Sends a password reset link if the address has an account. Same answer either way. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest request) {
        limits.acquire(Limit.MAIL_IP, request.getRemoteAddr());
        String email = UserAccounts.normalizeEmail(body.email());
        if (!UserAccounts.isValidEmail(email)) {
            throw new BadRequestException("Invalid e-mail address");
        }
        accounts.accountByEmail(email).ifPresent(account -> {
            if (limits.tryAcquire(Limit.MAIL_ADDRESS, account.email())) {
                String token = accounts.newEmailToken(account.id(), TokenPurpose.RESET, security.resetLinkTtl());
                mails.resetPassword(account.email(), account.username(), token, account.language());
            }
        });
    }

    /** New password from a reset link; logs the account out everywhere (the user then logs in with it). */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        String username = accounts.resetPassword(body.token(), body.password())
                .orElseThrow(() -> new BadRequestException("The link is invalid or expired"));
        limits.reset(Limit.LOGIN_USER, username);
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
        Account account = accounts.account(user.id());
        return new MeResponse(account.username(), account.email());
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

    /** New confirmation link, at most {@code mails-per-address} per window and address. */
    private void sendVerificationMail(Account account, String ip) {
        if (limits.tryAcquire(Limit.MAIL_ADDRESS, account.email()) && limits.tryAcquire(Limit.MAIL_IP, ip)) {
            String token = accounts.newEmailToken(account.id(), TokenPurpose.VERIFY, security.verifyLinkTtl());
            mails.verifyEmail(account.email(), account.username(), token, account.language());
        }
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
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try later"));
    }
}
