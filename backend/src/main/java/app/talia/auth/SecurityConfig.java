package app.talia.auth;

import app.talia.config.TaliaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    /** Health sync from the iOS Shortcut: stateless, static Bearer token. */
    @Bean
    @Order(1)
    SecurityFilterChain healthSyncChain(HttpSecurity http, TaliaProperties properties) throws Exception {
        return http
                .securityMatcher("/api/health-sync")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new SyncTokenFilter(properties.sync().tokenSha256()), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SYNC"))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    /**
     * Everything else: session cookie. CSRF tokens are disabled because the session cookie is SameSite=Strict
     * and the API only accepts JSON from the same origin.
     */
    @Bean
    @Order(2)
    SecurityFilterChain appChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/**").hasRole("USER")
                        .anyRequest().denyAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(TaliaProperties properties) {
        return new InMemoryUserDetailsManager(User.withUsername(properties.auth().username())
                .password(properties.auth().passwordHash())
                .roles("USER")
                .build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
