package app.talia.auth;

import app.talia.config.TaliaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Duration;

/**
 * The single definition of the session cookie, used by Spring Session and by {@code /api/auth/me} when it
 * re-issues the cookie. Defined explicitly so every attribute is visible in one place.
 */
@Configuration(proxyBeanMethods = false)
class SessionCookieConfig {

    static final String COOKIE_NAME = "TALIA_SESSION";
    static final Duration MAX_AGE = Duration.ofDays(365);

    @Bean
    DefaultCookieSerializer cookieSerializer(TaliaProperties properties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(COOKIE_NAME);
        serializer.setCookiePath("/");
        // not readable from JavaScript, so an XSS bug cannot steal the session
        serializer.setUseHttpOnlyCookie(true);
        // HTTPS only (false only for local http://localhost development)
        serializer.setUseSecureCookie(properties.security().cookieSecure());
        // never sent with requests started by other sites: this is the CSRF protection
        serializer.setSameSite("Strict");
        serializer.setCookieMaxAge((int) MAX_AGE.toSeconds());
        return serializer;
    }
}
