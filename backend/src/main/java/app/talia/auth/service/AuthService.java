package app.talia.auth.service;

import app.talia.auth.model.Account;
import app.talia.auth.model.EmailNotVerifiedException;
import app.talia.auth.model.EmailTakenException;
import app.talia.auth.model.RateLimit;
import app.talia.auth.model.RegisterRequest;
import app.talia.auth.model.TaliaUser;
import app.talia.auth.model.TokenPurpose;
import app.talia.common.BadRequestException;
import app.talia.config.TaliaProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Login, registration (ALTCHA bot check + e-mail confirmation) and password reset by e-mail, with the rate limits
 * (Bucket4j, see {@link RateLimitService}): failed logins per IP and per username, registrations per IP,
 * e-mails per IP and per address.
 * <p>
 * Nothing here reveals whether an e-mail address has an account: registration with a used address and
 * "forgot password" for an unknown address behave exactly like the successful case.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService accounts;
    private final EmailTokenService emailTokens;
    private final AccountMailService mails;
    private final BotCheckService botCheck;
    private final RateLimitService limits;
    private final TaliaProperties.Security security;

    AuthService(AuthenticationManager authenticationManager, UserAccountService accounts,
                EmailTokenService emailTokens, AccountMailService mails, BotCheckService botCheck,
                RateLimitService limits, TaliaProperties properties) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
        this.emailTokens = emailTokens;
        this.mails = mails;
        this.botCheck = botCheck;
        this.limits = limits;
        this.security = properties.security();
    }

    /**
     * Checks the password (with the failed-login limits). An account with an unconfirmed e-mail gets a fresh link
     * and {@link EmailNotVerifiedException}; this is only told after the right password, so it reveals nothing.
     */
    public Authentication login(String rawUsername, String password, String ip) {
        String username = UserAccountService.normalize(rawUsername);
        limits.check(RateLimit.LOGIN_IP, ip);
        limits.check(RateLimit.LOGIN_USER, username);
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException e) {
            limits.record(RateLimit.LOGIN_IP, ip);
            limits.record(RateLimit.LOGIN_USER, username);
            throw e;
        }
        limits.reset(RateLimit.LOGIN_USER, username);
        Account account = accounts.account(((TaliaUser) auth.getPrincipal()).id());
        if (!account.emailVerified()) {
            sendVerificationMail(account, ip);
            throw new EmailNotVerifiedException();
        }
        return auth;
    }

    /**
     * Creates an unconfirmed account and e-mails a confirmation link. Login works after confirming.
     * An address that already has an account gets a notice instead, and the caller sees no difference.
     */
    public void register(RegisterRequest request, String ip) {
        limits.acquire(RateLimit.REGISTER_IP, ip);
        if (request.website() != null && !request.website().isBlank()) {
            throw new BadRequestException("Registration rejected");
        }
        if (!botCheck.verify(request.altcha())) {
            throw new BadRequestException("Bot check failed, please try again");
        }
        String language = "en".equals(request.language()) ? "en" : "pl";
        try {
            TaliaUser user = accounts.create(request.username(), request.email(), request.password(), language);
            sendVerificationMail(accounts.account(user.id()), ip);
        } catch (EmailTakenException e) {
            accounts.accountByEmail(request.email()).ifPresent(existing -> {
                if (limits.tryAcquire(RateLimit.MAIL_ADDRESS, existing.email())) {
                    mails.alreadyRegistered(existing.email(), existing.username(), existing.language());
                }
            });
        }
    }

    /** Confirms the e-mail address with the token from the link. */
    public void verifyEmail(String token) {
        if (!emailTokens.verifyEmail(token)) {
            throw new BadRequestException("The link is invalid or expired");
        }
    }

    /** Sends a password reset link if the address has an account. Same outcome for the caller either way. */
    public void forgotPassword(String rawEmail, String ip) {
        limits.acquire(RateLimit.MAIL_IP, ip);
        String email = UserAccountService.normalizeEmail(rawEmail);
        if (!UserAccountService.isValidEmail(email)) {
            throw new BadRequestException("Invalid e-mail address");
        }
        accounts.accountByEmail(email).ifPresent(account -> {
            if (limits.tryAcquire(RateLimit.MAIL_ADDRESS, account.email())) {
                String token = emailTokens.newToken(account.id(), TokenPurpose.RESET, security.resetLinkTtl());
                mails.resetPassword(account.email(), account.username(), token, account.language());
            }
        });
    }

    /** New password from a reset link; the account is logged out everywhere and its login limit cleared. */
    public void resetPassword(String token, String password) {
        String username = emailTokens.resetPassword(token, password)
                .orElseThrow(() -> new BadRequestException("The link is invalid or expired"));
        limits.reset(RateLimit.LOGIN_USER, username);
    }

    /** New confirmation link, at most {@code mails-per-address} per window and address. */
    private void sendVerificationMail(Account account, String ip) {
        if (limits.tryAcquire(RateLimit.MAIL_ADDRESS, account.email()) && limits.tryAcquire(RateLimit.MAIL_IP, ip)) {
            String token = emailTokens.newToken(account.id(), TokenPurpose.VERIFY, security.verifyLinkTtl());
            mails.verifyEmail(account.email(), account.username(), token, account.language());
        }
    }
}
