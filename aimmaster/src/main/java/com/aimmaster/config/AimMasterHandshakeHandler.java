package com.aimmaster.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Turns the username that WebSocketAuthInterceptor already verified and stored in
 * the session attributes into a real java.security.Principal, so every STOMP
 * @MessageMapping method can just ask for "Principal principal" and get the
 * calling player's username - the same way an Authentication object works for
 * plain REST controllers (see JwtAuthenticationFilter).
 */
@Component
public class AimMasterHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        Object username = attributes.get(WebSocketAuthInterceptor.USERNAME_ATTRIBUTE);
        if (username == null) {
            // Should be unreachable: WebSocketAuthInterceptor rejects the handshake before
            // we ever get here if there's no valid token. Falling back to super() just
            // avoids a NullPointerException if that assumption is ever broken later.
            return super.determineUser(request, wsHandler, attributes);
        }
        String name = (String) username;
        return (Principal) () -> name;
    }
}
