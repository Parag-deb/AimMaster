package com.aimmaster.websocket;

import com.aimmaster.dto.FireBroadcast;
import com.aimmaster.dto.FireRequest;
import com.aimmaster.dto.LeaderboardBroadcast;
import com.aimmaster.dto.LeaderboardEntry;
import com.aimmaster.dto.TestMessageBroadcast;
import com.aimmaster.dto.TestMessageRequest;
import com.aimmaster.entity.Player;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Controller
public class RoomWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<Integer, Set<Integer>> roomPlayers = new ConcurrentHashMap<>();

    public RoomWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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
        roomPlayers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(playerId);

        FireBroadcast broadcast = new FireBroadcast(
                request.targetId(),
                playerId,
                10,
                System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, broadcast);

        List<LeaderboardEntry> standings = roomPlayers.getOrDefault(roomId, Set.of())
                .stream()
                .map(id -> new LeaderboardEntry(id, "Player" + id, 10))
                .collect(Collectors.toList());

        LeaderboardBroadcast leaderboard = new LeaderboardBroadcast(standings, System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, leaderboard);
    }
}