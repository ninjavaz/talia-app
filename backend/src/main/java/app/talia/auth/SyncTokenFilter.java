package app.talia.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Authenticates the iOS Shortcut by a static Bearer token. Only the SHA-256 of the token is configured and the
 * comparison is constant-time. Deliberately not a Spring bean, so it is not registered as a global servlet filter.
 */
class SyncTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final byte[] expectedHash;

    SyncTokenFilter(String tokenSha256Hex) {
        this.expectedHash = HexFormat.of().parseHex(tokenSha256Hex.toLowerCase(Locale.ROOT));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            if (MessageDigest.isEqual(sha256(token), expectedHash)) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        "health-sync", null, AuthorityUtils.createAuthorityList("ROLE_SYNC")));
                SecurityContextHolder.setContext(context);
            }
        }
        chain.doFilter(request, response);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
