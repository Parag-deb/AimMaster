package com.aimmaster.config;

import com.aimmaster.entity.Player;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Intercepts STOMP messages to track session-to-player and session-to-room
 * mappings. Registered on CONNECT, SUBSCRIBE, and UNSUBSCRIBE.
 */
@Component
public class WebSocketSessionInterceptor implements ChannelInterceptor {

    private final WebSocketSessionTracker tracker;

    public WebSocketSessionInterceptor(WebSocketSessionTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT:
                Object playerAttr = accessor.getSessionAttributes().get("player");
                if (playerAttr instanceof Player player) {
                    tracker.registerSession(sessionId, player.getId());
                }
                break;
            case SUBSCRIBE:
                String destination = accessor.getDestination();
                if (destination != null && destination.startsWith("/topic/room/")) {
                    try {
                        Integer roomId = Integer.parseInt(destination.substring("/topic/room/".length()));
                        tracker.addRoomSubscription(sessionId, roomId);
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            case UNSUBSCRIBE:
                String unsubDest = accessor.getDestination();
                if (unsubDest != null && unsubDest.startsWith("/topic/room/")) {
                    try {
                        Integer roomId = Integer.parseInt(unsubDest.substring("/topic/room/".length()));
                        tracker.removeRoomSubscription(sessionId, roomId);
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            default:
                break;
        }
        return message;
    }
}
