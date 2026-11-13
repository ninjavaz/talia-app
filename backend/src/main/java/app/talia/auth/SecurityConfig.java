package app.talia.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    /** Health sync from the iOS Shortcut: stateless, personal Bearer token per user. */
    @Bean
    @Order(1)
    SecurityFilterChain healthSyncChain(HttpSecurity http, UserAccounts accounts) throws Exception {
        return http
                .securityMatcher("/api/health-sync")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new SyncTokenFilter(accounts), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SYNC"))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    /**
     * Everything else: session cookie (HttpOnly, Secure, SameSite=Strict). CSRF tokens are not needed because the
     * cookie is never sent on cross-site requests and the API only accepts JSON from the same origin.
     */
    @Bean
    @Order(2)
    SecurityFilterChain appChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/challenge",
                                "/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/**").hasRole("USER")
                        .anyRequest().denyAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** bcrypt with cost 12 (~250 ms per check on a small VPS: cheap for a login, expensive for offline cracking). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));
    }

    @Bean
    AuthenticationManager authenticationManager(UserAccounts accounts, PasswordEncoder passwordEncoder) {
        // DaoAuthenticationProvider also hashes a dummy password for unknown usernames,
        // so response times do not reveal which accounts exist.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(accounts);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
