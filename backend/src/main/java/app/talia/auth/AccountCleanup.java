package app.talia.auth;

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
class AccountCleanup {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanup.class);

    private final UserAccounts accounts;
    private final TaliaProperties.Security security;

    AccountCleanup(UserAccounts accounts, TaliaProperties properties) {
        this.accounts = accounts;
        this.security = properties.security();
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT2M")
    void run() {
        int users = accounts.deleteUnverifiedOlderThan(security.verifyLinkTtl());
        int tokens = accounts.deleteExpiredEmailTokens();
        if (users > 0 || tokens > 0) {
            log.info("Cleanup: {} unconfirmed accounts, {} expired e-mail links deleted", users, tokens);
        }
    }
}
