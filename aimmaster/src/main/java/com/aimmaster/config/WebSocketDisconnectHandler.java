package com.aimmaster.config;

import com.aimmaster.service.GameSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Handles WebSocket STOMP disconnect events.
 * Removes the disconnected player from their room(s) and stops
 * the match if no players remain.
 */
@Component
public class WebSocketDisconnectHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDisconnectHandler.class);

    private final WebSocketSessionTracker tracker;
    private final GameSessionManager gameSessionManager;

    public WebSocketDisconnectHandler(WebSocketSessionTracker tracker,
                                       GameSessionManager gameSessionManager) {
        this.tracker = tracker;
        this.gameSessionManager = gameSessionManager;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        WebSocketSessionTracker.SessionInfo info = tracker.unregister(sessionId);
        if (info == null) {
            return;
        }

        Integer playerId = info.playerId();
        java.util.Set<Integer> roomIds = info.roomIds();

        log.info("Player {} disconnected from rooms {}", playerId, roomIds);

        for (Integer roomId : roomIds) {
            java.util.Set<Integer> remaining = tracker.getPlayersInRoom(roomId);
            log.info("Room {}: player {} left, {} players remaining", roomId, playerId, remaining.size());

            if (remaining.isEmpty()) {
                log.info("Room {} has no players remaining, stopping match", roomId);
                gameSessionManager.stopMatch(roomId);
            }
        }
    }
}
