package com.aimmaster.websocket;

import com.aimmaster.dto.FireEventMessage;
import com.aimmaster.dto.HitResultMessage;
import com.aimmaster.dto.WsTestMessage;
import com.aimmaster.model.Target;
import com.aimmaster.service.GameSession;
import com.aimmaster.service.GameSessionManager;
import com.aimmaster.service.HitResolver;
import com.aimmaster.service.RoomBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

/**
 * Handles inbound STOMP messages for a room: Sprint 3's transport test, and (Sprint 5)
 * FR5's real "fire" event - a player claiming they hit a target.
 *
 * "@MessageMapping" is the STOMP equivalent of "@PostMapping" for REST - it maps an
 * inbound destination to a handler method. "@SendTo" (with the same {roomId} used in
 * @DestinationVariable) sends the return value to that specific room's topic, so a
 * message sent for room "ABC123" only reaches players subscribed to room "ABC123" -
 * never leaking into other rooms' broadcasts. Sprint 3's test handler is left exactly
 * as it was (still plain @SendTo, player topic only) - it's a deliberate throwaway
 * diagnostic, not part of the real game flow, so it was never updated to also reach
 * spectators. The fire handler below sends manually via RoomBroadcaster instead of
 * @SendTo, because on a miss it must send NOTHING at all (the Sprint 5 payload format
 * is only defined for confirmed hits), and it needs to reach both the player topic
 * and (Sprint 9) the spectator topic.
 */
@Controller
public class RoomWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(RoomWebSocketController.class);

    private final GameSessionManager gameSessionManager;
    private final HitResolver hitResolver;
    private final RoomBroadcaster roomBroadcaster;

    public RoomWebSocketController(GameSessionManager gameSessionManager, HitResolver hitResolver,
                                    RoomBroadcaster roomBroadcaster) {
        this.gameSessionManager = gameSessionManager;
        this.hitResolver = hitResolver;
        this.roomBroadcaster = roomBroadcaster;
    }

    @MessageMapping("/room/{roomId}/test")
    @SendTo("/topic/room/{roomId}")
    public WsTestMessage handleTestMessage(@DestinationVariable String roomId,
                                            WsTestMessage incoming,
                                            Principal principal) {

        log.info("Test message in room {} from {}: {}", roomId, principal.getName(), incoming.getContent());

        incoming.setSenderUsername(principal.getName());
        incoming.setServerTimestamp(Instant.now().toEpochMilli());
        return incoming;
    }

    /**
     * FR5/FR13/FR14: a player's thumb-fold gesture claims a hit on one target. The
     * server - never the client - decides whether that claim actually counts, via
     * HitResolver's atomic check. On a genuine hit, the score updates and every
     * player in the room finds out via a broadcast (FR7). On a miss (bad/expired
     * target id, or someone else's shot already won the race a moment earlier),
     * nothing at all is sent - a flood of "miss" broadcasts would just be noise.
     */
    @MessageMapping("/room/{roomId}/fire")
    public void handleFireEvent(@DestinationVariable String roomId,
                                 FireEventMessage fireEvent,
                                 Principal principal) {

        GameSession session = gameSessionManager.getSession(roomId);
        if (session == null) {
            // No match is currently running in this room (host hasn't started it yet,
            // or it already ended) - there's nothing to shoot at, so just ignore it.
            log.warn("Fire event for room {} ignored - no match is currently running", roomId);
            return;
        }

        // Counted whether this turns out to be a hit or a miss - Sprint 6's accuracy
        // stat (hits / total attempts) needs the miss count too, not just the hits.
        session.recordFireAttempt(principal.getName());

        Target hitTarget = hitResolver.resolveHit(session.getArena(), fireEvent.getTargetId());
        if (hitTarget == null) {
            return; // miss - see method javadoc for why we deliberately send nothing back
        }

        int newScore = session.getLeaderboard().addPoint(principal.getName());
        HitResultMessage result = new HitResultMessage(
                hitTarget.getId(), principal.getName(), newScore, Instant.now().toEpochMilli());

        log.info("Room {}: {} destroyed target {} (score now {})",
                roomId, principal.getName(), hitTarget.getId(), newScore);

        roomBroadcaster.broadcastToRoom(roomId, result);
    }
}
