package app.talia;

import app.talia.auth.model.TaliaUser;
import app.talia.auth.service.MailService;
import app.talia.auth.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base class: full Spring context against a real Postgres (shared container via the cached context).
 * "Now" is fixed to {@link #NOW}: Sunday 15 Nov 2026, 10:00 in Warsaw (week Mon 9 - Sun 15 Nov).
 * <p>
 * Every test starts with one account "talia" (id 1, password {@link #PASSWORD}, confirmed e-mail
 * talia@example.com) that has the built-in waist
 * (body part 1), three habits (ids 1-3) and the sync token {@link #SYNC_TOKEN}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IntegrationTest.FixedClock.class})
public abstract class IntegrationTest {

    protected static final ZonedDateTime NOW = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
    protected static final LocalDate TODAY = NOW.toLocalDate();
    protected static final String PASSWORD = "Tajne-Haslo-2026";
    /** Personal Apple Health sync token of "talia" (only its SHA-256 is stored). */
    protected static final String SYNC_TOKEN = "test-sync-token-0123456789abcdef";
    private static final String SYNC_TOKEN_SHA256 = "5b1174601e24c09eb56960e451d74c7d37a202860af36003bd6c7b4b7ecb69ce";

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW.toInstant(), NOW.getZone());
        }
    }

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected UserAccountService accounts;

    /** E-mails are not sent in tests; tests read them from this mock. */
    @MockitoBean
    protected MailService mailer;

    protected MockMvc mvc;
    protected TaliaUser talia;

    @BeforeEach
    void setUpMockMvcAndCleanData() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // deleting the users cascades to all their data
        jdbc.sql("TRUNCATE app_user, spring_session, rate_limit_bucket, altcha_used RESTART IDENTITY CASCADE").update();
        talia = accounts.create("talia", PASSWORD);
        jdbc.sql("UPDATE app_user SET sync_token_hash = :hash WHERE id = :id")
                .param("hash", SYNC_TOKEN_SHA256).param("id", talia.id()).update();
        jdbc.sql("""
                INSERT INTO habit (user_id, name, sort_order) VALUES
                    (:id, 'Białko w posiłkach', 1), (:id, 'Warzywa do 2 posiłków', 2), (:id, 'Bez słodkich napojów', 3)
                """).param("id", talia.id()).update();
    }

    /** Requests as the default user "talia". */
    protected RequestPostProcessor asUser() {
        return as(talia);
    }

    protected static RequestPostProcessor as(TaliaUser user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    /** Simulates a request from the given client IP (rate limits are per IP). */
    protected static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
