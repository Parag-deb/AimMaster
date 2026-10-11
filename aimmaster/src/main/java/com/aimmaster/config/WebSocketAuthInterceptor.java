package com.aimmaster.config;

import com.aimmaster.service.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * DECISION (Sprint 3 pitfall: "decide explicitly how JWT is validated on WebSocket
 * connect, don't leave it unauthenticated"):
 *
 * A browser's native WebSocket/SockJS client can't attach a custom "Authorization"
 * header the way a normal REST fetch() call can, so instead the client connects to
 * "/ws?token=<jwt>" and we read the token from the handshake URL's query string here,
 * BEFORE the WebSocket connection is even accepted.
 *
 * If the token is missing/invalid, beforeHandshake() returns false and Spring rejects
 * the connection outright (the browser sees the handshake fail) - a bad/expired token
 * never gets as far as subscribing to a room topic.
 *
 * If it's valid, we stash the username in `attributes`, which Spring copies onto the
 * WebSocketSession - our AimMasterHandshakeHandler (below) then turns that into a real
 * java.security.Principal for every @MessageMapping method to use.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    static final String USERNAME_ATTRIBUTE = "username";

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {

        List<String> tokenParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .get("token");

        String token = (tokenParams == null || tokenParams.isEmpty()) ? null : tokenParams.get(0);

        if (token == null || !jwtService.isTokenValid(token)) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false; // reject the handshake - connection never opens
        }

        attributes.put(USERNAME_ATTRIBUTE, jwtService.extractUsername(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after a successful handshake.
    }
}
