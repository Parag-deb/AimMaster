package com.aimmaster.websocket;

import com.aimmaster.dto.PlayerLeftMessage;
import com.aimmaster.exception.MatchNotRunningException;
import com.aimmaster.service.GameSessionManager;
import com.aimmaster.service.RoomBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FR16: "disconnected players are removed from the room without crashing the
 * session for others." Spring publishes SessionSubscribeEvent/SessionDisconnectEvent
 * on the whole application whenever a STOMP client subscribes/disconnects - this
 * class just listens for both and reacts. It never touches the persisted Room
 * membership (room_players) or GameSession's original participant snapshot - a
 * dropped WebSocket is treated as "temporarily gone," not "quit the room forever"
 * (Sprint 9's reconnect handling assumes the same thing).
 *
 * We record which room a session cares about at SUBSCRIBE time (not CONNECT time),
 * because the JWT handshake alone doesn't say which room a client wants - it only
 * finds that out once the client subscribes to a room's topic, exactly like our own
 * game.js does right after connecting.
 *
 * Sprint 9 (spectator mode) note: a spectator subscribes to a DIFFERENT topic
 * (".../spectators" - see RoomBroadcaster) than a player. Spectators are tracked
 * separately and deliberately never affect match lifecycle or trigger a "player
 * left" notice - watching a match end when the last player leaves would be strange
 * if a spectator leaving accidentally ended it instead.
 */
@Component
public class RoomPresenceListener {

    private static final Logger log = LoggerFactory.getLogger(RoomPresenceListener.class);
    private static final String ROOM_TOPIC_PREFIX = "/topic/room/";
    private static final String SPECTATOR_SUFFIX = "/spectators";

    private final GameSessionManager gameSessionManager;
    private final RoomBroadcaster roomBroadcaster;

    /**
     * Which room (and whether as a spectator) each STOMP session is watching. A
     * small private record beats a plain String here - a spectator subscribing to
     * "/topic/room/ABC123/spectators" must NOT be parsed as room code
     * "ABC123/spectators" (a real bug caught while building this: naively stripping
     * only the ROOM_TOPIC_PREFIX would leave the "/spectators" suffix stuck onto the
     * room code). The suffix is stripped explicitly below, and which case it was is
     * remembered here so onDisconnect knows which bookkeeping map to clean up.
     */
    private record SessionRoomInfo(String roomCode, boolean spectator) {
    }

    private final Map<String, SessionRoomInfo> roomBySessionId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> playerSessionIdsByRoom = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> spectatorSessionIdsByRoom = new ConcurrentHashMap<>();

    public RoomPresenceListener(GameSessionManager gameSessionManager, RoomBroadcaster roomBroadcaster) {
        this.gameSessionManager = gameSessionManager;
        this.roomBroadcaster = roomBroadcaster;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return; // some other subscription we don't need to track
        }

        String remainder = destination.substring(ROOM_TOPIC_PREFIX.length()); // "ABC123" or "ABC123/spectators"
        boolean isSpectator = remainder.endsWith(SPECTATOR_SUFFIX);
        String roomCode = isSpectator
                ? remainder.substring(0, remainder.length() - SPECTATOR_SUFFIX.length())
                : remainder;
        String sessionId = accessor.getSessionId();

        roomBySessionId.put(sessionId, new SessionRoomInfo(roomCode, isSpectator));

        Map<String, Set<String>> targetMap = isSpectator ? spectatorSessionIdsByRoom : playerSessionIdsByRoom;
        targetMap.computeIfAbsent(roomCode, code -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        SessionRoomInfo info = roomBySessionId.remove(sessionId);
        if (info == null) {
            return; // this session never subscribed to a room's topic - nothing to clean up
        }

        String username = event.getUser() != null ? event.getUser().getName() : "someone";

        if (info.spectator()) {
            removeFromMap(spectatorSessionIdsByRoom, info.roomCode(), sessionId);
            log.info("{} stopped spectating room {}", username, info.roomCode());
            return; // spectators never affect match lifecycle or generate a "player left" notice
        }

        Set<String> remainingPlayers = removeFromMap(playerSessionIdsByRoom, info.roomCode(), sessionId);
        boolean roomNowEmptyOfPlayers = remainingPlayers == null || remainingPlayers.isEmpty();

        if (roomNowEmptyOfPlayers) {
            // Deliberately NOT "if (isRunning(roomCode)) stopMatch(roomCode)" - that would be
            // a check-then-act race against the match's own duration-expiry timer, which can
            // stop (and remove) this same session on a different thread at any moment. Same
            // family of bug Target.tryMarkDead() exists to prevent in Sprint 4/5: instead we
            // just attempt the stop and treat "there wasn't one running" as a normal, harmless
            // outcome - not an error - since that only means we lost a harmless race, not that
            // anything went wrong.
            try {
                gameSessionManager.stopMatch(info.roomCode());
                log.info("Room {} emptied out of players mid-match (last one, {}, disconnected) - stopping its match",
                        info.roomCode(), username);
            } catch (MatchNotRunningException ignored) {
                // No match was running here (or it had just ended on its own) - nothing to do.
            }
        } else {
            log.info("{} disconnected from room {} - {} player(s) remain", username, info.roomCode(), remainingPlayers.size());
            roomBroadcaster.broadcastToRoom(info.roomCode(), new PlayerLeftMessage(username));
        }
    }

    /** Removes sessionId from roomCode's set in the given map, deleting the set entirely once empty. */
    private Set<String> removeFromMap(Map<String, Set<String>> map, String roomCode, String sessionId) {
        Set<String> sessions = map.get(roomCode);
        if (sessions == null) {
            return null;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            map.remove(roomCode);
            return null;
        }
        return sessions;
    }

    /** Sprint 9 (admin dashboard): a snapshot of how many players are currently connected to each room. */
    public Map<String, Integer> getConnectedPlayerCountsByRoom() {
        Map<String, Integer> counts = new HashMap<>();
        playerSessionIdsByRoom.forEach((roomCode, sessions) -> counts.put(roomCode, sessions.size()));
        return Collections.unmodifiableMap(counts);
    }

    /** Sprint 9 (admin dashboard): a snapshot of how many spectators are currently watching each room. */
    public Map<String, Integer> getSpectatorCountsByRoom() {
        Map<String, Integer> counts = new HashMap<>();
        spectatorSessionIdsByRoom.forEach((roomCode, sessions) -> counts.put(roomCode, sessions.size()));
        return Collections.unmodifiableMap(counts);
    }
}
