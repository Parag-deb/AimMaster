package com.aimmaster.config;

import com.aimmaster.entity.Player;
import com.aimmaster.repository.PlayerRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WebSocketJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final PlayerRepository playerRepository;

    public WebSocketJwtHandshakeInterceptor(JwtUtil jwtUtil, PlayerRepository playerRepository) {
        this.jwtUtil = jwtUtil;
        this.playerRepository = playerRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            return false;
        }

        try {
            var claims = jwtUtil.parseToken(token);
            Integer playerId = Integer.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);

            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (playerOpt.isEmpty() || !playerOpt.get().getUsername().equals(username)) {
                return false;
            }

            Player player = playerOpt.get();
            var auth = new UsernamePasswordAuthenticationToken(
                    player,
                    token,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            attributes.put("player", player);
            attributes.put("playerId", playerId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        SecurityContextHolder.clearContext();
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        List<String> tokenParams = request.getHeaders().get("sec-websocket-protocol");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            String protocol = tokenParams.get(0);
            if (protocol.startsWith("Bearer ")) {
                return protocol.substring(7);
            }
        }

        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }
}