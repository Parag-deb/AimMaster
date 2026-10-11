package com.aimmaster.config;

import com.aimmaster.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per incoming REST request, BEFORE it reaches any @RestController.
 * Job: look for an "Authorization: Bearer <token>" header, and if it's a valid JWT,
 * tell Spring Security "this request is from this logged-in username" so
 * @RestController methods can trust request.getUserPrincipal() / SecurityContext.
 *
 * "OncePerRequestFilter" just guarantees Spring won't accidentally run this filter
 * twice on the same request (can happen with internal forwards/includes).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER_NAME);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);

                // No roles/permissions system yet - every logged-in player has the same
                // single "PLAYER" authority. Empty authorities would also work; this makes
                // intent explicit for whoever reads this next.
                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(() -> "ROLE_PLAYER"));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // An invalid/expired token is simply ignored here - the request continues as
            // "anonymous," and SecurityConfig decides whether that's allowed for this path.
        }

        filterChain.doFilter(request, response);
    }
}
