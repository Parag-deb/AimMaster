package com.aimmaster.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Sprint 9 (spectator mode): every broadcast a room produces (arena state, hit
 * results, "player left" notices) now needs to reach TWO audiences - the players
 * actually in the match, and anyone spectating it - over two separate topics. Before
 * this class existed, each of GameSession, RoomWebSocketController and
 * RoomPresenceListener called messagingTemplate.convertAndSend("/topic/room/" + code,
 * ...) directly; adding a second topic would have meant editing that same line in
 * three different places. Centralizing it here means "how a room's messages reach
 * its audience" is decided in exactly one spot - see
 * AimMaster_03_Agent_Instructions.md Section 3: "shared logic goes in one place."
 */
@Component
public class RoomBroadcaster {

    private static final String PLAYER_TOPIC_PREFIX = "/topic/room/";
    private static final String SPECTATOR_TOPIC_SUFFIX = "/spectators";

    private final SimpMessagingTemplate messagingTemplate;

    public RoomBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Sends the same payload to both this room's player topic and its spectator topic. */
    public void broadcastToRoom(String roomCode, Object payload) {
        messagingTemplate.convertAndSend(playerTopic(roomCode), payload);
        messagingTemplate.convertAndSend(spectatorTopic(roomCode), payload);
    }

    public static String playerTopic(String roomCode) {
        return PLAYER_TOPIC_PREFIX + roomCode;
    }

    public static String spectatorTopic(String roomCode) {
        return PLAYER_TOPIC_PREFIX + roomCode + SPECTATOR_TOPIC_SUFFIX;
    }
}
