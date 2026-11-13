package app.talia;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base class: full Spring context against a real Postgres (shared container via the cached context).
 * "Now" is fixed to {@link #NOW}: Sunday 15 Nov 2026, 10:00 in Warsaw (week Mon 9 - Sun 15 Nov).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IntegrationTest.FixedClock.class})
public abstract class IntegrationTest {

    protected static final ZonedDateTime NOW = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
    protected static final LocalDate TODAY = NOW.toLocalDate();

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

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvcAndCleanData() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.sql("""
                TRUNCATE daily_steps, weekly_steps, weight_entry, body_part, body_measurement, workout, sync_log,
                         habit, habit_check
                RESTART IDENTITY CASCADE
                """).update();
        // body part 1 = built-in waist
        jdbc.sql("INSERT INTO body_part (name, code, sort_order) VALUES ('Talia', 'WAIST', 0)").update();
        jdbc.sql("""
                INSERT INTO habit (name, sort_order) VALUES
                    ('Białko w posiłkach', 1), ('Warzywa do 2 posiłków', 2), ('Bez słodkich napojów', 3)
                """).update();
        jdbc.sql("""
                UPDATE app_settings SET weekly_workout_goal = 3, daily_steps_goal = 8000,
                    habit_week_threshold_pct = 70, language = 'pl'
                """).update();
    }

    /** Logged-in user for MockMvc requests. */
    protected static RequestPostProcessor asUser() {
        return user("talia").roles("USER");
    }
}
