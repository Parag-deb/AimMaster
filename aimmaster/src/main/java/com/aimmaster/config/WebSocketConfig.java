package com.aimmaster.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Turns on the real-time transport (FR6/FR7: everyone in a room sees the same live
 * arena/score updates). This is the ONLY place the topic naming convention is defined -
 * locked here in Sprint 3, never redefined in a later sprint (see
 * AimMaster_03_Agent_Instructions.md Section 4):
 *
 *   /topic/room/{roomId}      -> server broadcasts TO all clients in that room
 *   /app/room/{roomId}/...    -> messages clients send INTO the server
 *
 * "@EnableWebSocketMessageBroker" turns on STOMP-over-WebSocket messaging (think of
 * STOMP as giving plain WebSocket the same "subscribe to a topic / send to a
 * destination" pub-sub shape that makes broadcasting to a room simple).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final AimMasterHandshakeHandler handshakeHandler;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor,
                            AimMasterHandshakeHandler handshakeHandler) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.handshakeHandler = handshakeHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Any browser can connect from any origin during development. Tighten this to
                // the real frontend's origin before deploying anywhere public.
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor)
                .setHandshakeHandler(handshakeHandler)
                // SockJS falls back to older transports automatically if a browser/proxy
                // doesn't support raw WebSocket - safer default for a student demo environment.
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // A "simple broker" keeps subscriptions in server memory - perfect for our scale
        // (a handful of rooms, a few players each). A production system with many server
        // instances would swap this for an external broker (RabbitMQ/ActiveMQ) - explicitly
        // out of scope per AimMaster_01_Project_Overview.md Section 4.
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
