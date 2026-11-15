package app.talia.e2e;

import app.talia.auth.service.UserAccountService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Test-only endpoints for the Playwright e2e tests. {@code POST /api/e2e/reset} wipes all data and creates the
 * account {@value #USERNAME} (confirmed e-mail, password {@value #PASSWORD}), so every e2e test starts clean.
 * Registered only by {@link E2eTaliaApplication} and only with the "e2e" profile.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("e2e")
public class E2eSupport {

    public static final String USERNAME = "e2e";
    public static final String PASSWORD = "Spokojny-Tydzien-2026";

    /** Runs before the application chain, which would require a login for /api/**. */
    @Bean
    @Order(0)
    SecurityFilterChain e2eChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/e2e/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /**
     * A functional endpoint on purpose: an annotated controller would also be found by component scanning
     * (the test classes are on the classpath) and show up in the other tests.
     */
    @Bean
    RouterFunction<ServerResponse> e2eReset(JdbcClient jdbc, UserAccountService accounts) {
        return RouterFunctions.route()
                .POST("/api/e2e/reset", request -> {
                    // deleting the users cascades to all their data
                    jdbc.sql("TRUNCATE app_user, spring_session, rate_limit_bucket, altcha_used RESTART IDENTITY CASCADE")
                            .update();
                    accounts.create(USERNAME, PASSWORD);
                    return ServerResponse.noContent().build();
                })
                .build();
    }
}
