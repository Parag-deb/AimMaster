package com.aimmaster.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks WebSocket STOMP sessions to players and rooms.
 * Maintains bidirectional mappings: session → player, session → rooms,
 * and room → players, so disconnects can clean up state reliably.
 */
@Component
public class WebSocketSessionTracker {

    private final Map<String, Integer> sessionToPlayer = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> sessionToRooms = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> roomToPlayers = new ConcurrentHashMap<>();

    /**
     * Register a session with its player when STOMP CONNECT is received.
     */
    public void registerSession(String sessionId, Integer playerId) {
        sessionToPlayer.put(sessionId, playerId);
    }

    /**
     * Record that a session is now subscribed to a room topic.
     */
    public void addRoomSubscription(String sessionId, Integer roomId) {
        Integer playerId = sessionToPlayer.get(sessionId);
        if (playerId == null) {
            return;
        }
        sessionToRooms.computeIfAbsent(sessionId, k -> new HashSet<>()).add(roomId);
        roomToPlayers.computeIfAbsent(roomId, k -> new HashSet<>()).add(playerId);
    }

    /**
     * Remove a room subscription.
     */
    public void removeRoomSubscription(String sessionId, Integer roomId) {
        Integer playerId = sessionToPlayer.get(sessionId);
        Set<Integer> rooms = sessionToRooms.get(sessionId);
        if (rooms != null) {
            rooms.remove(roomId);
        }
        Set<Integer> players = roomToPlayers.get(roomId);
        if (players != null && playerId != null) {
            players.remove(playerId);
        }
    }

    /**
     * Unregister a session on disconnect. Returns the session info, or null if unknown.
     */
    public SessionInfo unregister(String sessionId) {
        Integer playerId = sessionToPlayer.remove(sessionId);
        Set<Integer> roomIds = sessionToRooms.remove(sessionId);
        if (playerId == null) {
            return null;
        }
        if (roomIds != null) {
            for (Integer roomId : roomIds) {
                Set<Integer> players = roomToPlayers.get(roomId);
                if (players != null) {
                    players.remove(playerId);
                }
            }
        }
        return new SessionInfo(playerId, roomIds != null ? roomIds : Collections.emptySet());
    }

    /**
     * Get all player IDs currently in a room (across all sessions).
     */
    public Set<Integer> getPlayersInRoom(Integer roomId) {
        Set<Integer> players = roomToPlayers.get(roomId);
        return players != null ? players : Collections.emptySet();
    }

    public Integer getPlayerId(String sessionId) {
        return sessionToPlayer.get(sessionId);
    }

    public Set<Integer> getRoomIds(String sessionId) {
        return sessionToRooms.getOrDefault(sessionId, Collections.emptySet());
    }

    public record SessionInfo(Integer playerId, Set<Integer> roomIds) {
    }
}
