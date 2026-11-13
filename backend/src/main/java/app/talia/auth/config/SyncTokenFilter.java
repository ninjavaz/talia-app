package app.talia.auth.config;

import app.talia.auth.model.SyncPrincipal;
import app.talia.auth.service.SyncTokenService;
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

/**
 * Authenticates the iOS Shortcut by the user's personal Bearer token (see {@link SyncTokenService}).
 * Deliberately not a Spring bean (no global servlet filter), only added to the health-sync chain.
 */
class SyncTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final SyncTokenService syncTokens;

    SyncTokenFilter(SyncTokenService syncTokens) {
        this.syncTokens = syncTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            syncTokens.userIdFor(token).ifPresent(userId -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        new SyncPrincipal(userId), null, AuthorityUtils.createAuthorityList("ROLE_SYNC")));
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }
}
