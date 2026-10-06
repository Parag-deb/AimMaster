package com.aimmaster.websocket;

import com.aimmaster.dto.FireRequest;
import com.aimmaster.dto.FireBroadcast;
import com.aimmaster.dto.StartMatchRequest;
import com.aimmaster.dto.TestMessageBroadcast;
import com.aimmaster.dto.TestMessageRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.service.FireRateLimiter;
import com.aimmaster.service.GameSession;
import com.aimmaster.service.GameSessionManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class RoomWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionManager gameSessionManager;
    private final FireRateLimiter fireRateLimiter;

    public RoomWebSocketController(SimpMessagingTemplate messagingTemplate,
                                    GameSessionManager gameSessionManager,
                                    FireRateLimiter fireRateLimiter) {
        this.messagingTemplate = messagingTemplate;
        this.gameSessionManager = gameSessionManager;
        this.fireRateLimiter = fireRateLimiter;
    }

    @MessageMapping("/room/{roomId}/test")
    @SendTo("/topic/room/{roomId}")
    public TestMessageBroadcast handleTestMessage(@AuthenticationPrincipal Player player,
                                                   TestMessageRequest request) {
        return new TestMessageBroadcast(
                player.getUsername(),
                request.content(),
                System.currentTimeMillis()
        );
    }

    @MessageMapping("/room/{roomId}/fire")
    public void handleFire(@AuthenticationPrincipal Player player,
                           Integer roomId,
                           FireRequest request) {
        Integer playerId = player.getId();

        if (!fireRateLimiter.tryAcquire(roomId, playerId)) {
            messagingTemplate.convertAndSendToUser(
                    player.getUsername(),
                    "/queue/errors",
                    "Fire rate limited — slow down"
            );
            return;
        }

        gameSessionManager.handleHit(roomId, request.targetId(), playerId);
    }

    @MessageMapping("/room/{roomId}/start")
    public void handleStartMatch(@AuthenticationPrincipal Player player,
                                 Integer roomId,
                                 StartMatchRequest request) {
        Integer playerId = player.getId();
        if (gameSessionManager.isRoomHost(roomId, playerId)) {
            gameSessionManager.startMatch(roomId);
        } else {
            messagingTemplate.convertAndSendToUser(player.getUsername(), "/queue/errors", 
                "Only the room host can start the match");
        }
    }
}