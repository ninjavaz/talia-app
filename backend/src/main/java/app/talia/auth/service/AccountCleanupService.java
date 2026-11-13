package app.talia.auth.service;

import app.talia.config.TaliaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly: deletes accounts whose e-mail was never confirmed (so nobody can block a username or someone else's
 * address with a fake registration for longer than the link validity) and expired e-mail links.
 */
@Component
public class AccountCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanupService.class);

    private final UserAccountService accounts;
    private final EmailTokenService emailTokens;
    private final TaliaProperties.Security security;

    AccountCleanupService(UserAccountService accounts, EmailTokenService emailTokens, TaliaProperties properties) {
        this.accounts = accounts;
        this.emailTokens = emailTokens;
        this.security = properties.security();
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT2M")
    public void run() {
        int users = accounts.deleteUnverifiedOlderThan(security.verifyLinkTtl());
        int tokens = emailTokens.deleteExpired();
        if (users > 0 || tokens > 0) {
            log.info("Cleanup: {} unconfirmed accounts, {} expired e-mail links deleted", users, tokens);
        }
    }
}
