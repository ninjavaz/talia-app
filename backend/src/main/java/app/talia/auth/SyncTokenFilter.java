package app.talia.auth;

import app.talia.common.Hashes;
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
 * Authenticates the iOS Shortcut by the user's personal Bearer token. Only the SHA-256 of each token is stored;
 * the lookup is by hash, so the token itself never touches the database. The token is 256 bits of randomness,
 * so a fast hash is sufficient (unlike passwords). Deliberately not a Spring bean (no global servlet filter).
 */
class SyncTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final UserAccounts accounts;

    SyncTokenFilter(UserAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            Long userId = token.length() >= 20 ? accounts.userIdForSyncTokenHash(Hashes.sha256(token)) : null;
            if (userId != null) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        new SyncPrincipal(userId), null, AuthorityUtils.createAuthorityList("ROLE_SYNC")));
                SecurityContextHolder.setContext(context);
            }
        }
        chain.doFilter(request, response);
    }
}
