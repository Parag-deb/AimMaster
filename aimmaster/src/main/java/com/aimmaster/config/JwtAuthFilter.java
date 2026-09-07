package com.aimmaster.config;

import com.aimmaster.entity.Player;
import com.aimmaster.repository.PlayerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PlayerRepository playerRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, PlayerRepository playerRepository) {
        this.jwtUtil = jwtUtil;
        this.playerRepository = playerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            var claims = jwtUtil.parseToken(token);
            Integer playerId = Integer.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);

            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (playerOpt.isPresent() && playerOpt.get().getUsername().equals(username)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        playerOpt.get(),
                        token,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
