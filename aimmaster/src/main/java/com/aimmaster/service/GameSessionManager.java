package com.aimmaster.service;

import com.aimmaster.entity.Room;
import com.aimmaster.exception.MatchAlreadyRunningException;
import com.aimmaster.exception.MatchNotRunningException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The bookkeeper: which rooms currently have a match running, and their GameSession.
 * This is the ONLY class that creates/removes GameSession instances, which is what
 * guarantees isolation - each room gets exactly one GameSession, each with its own
 * thread (see GameSession's javadoc).
 *
 * Only the small map operations here are synchronized - never the game loop itself.
 * A game loop tick runs entirely on its own GameSession's own executor thread, so
 * synchronizing "is a session already running for this room code?" for a few
 * microseconds never blocks or slows down any room's actual ticking.
 */
@Service
public class GameSessionManager {

    private final GameEngine gameEngine;
    private final RoomBroadcaster roomBroadcaster; // Sprint 9: fans broadcasts out to players + spectators
    private final MatchRecordService matchRecordService; // Sprint 6: persists results when a match ends
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    public GameSessionManager(GameEngine gameEngine, RoomBroadcaster roomBroadcaster,
                               MatchRecordService matchRecordService) {
        this.gameEngine = gameEngine;
        this.roomBroadcaster = roomBroadcaster;
        this.matchRecordService = matchRecordService;
    }

    public synchronized void startMatch(Room room, Set<String> participantUsernames) {
        if (activeSessions.containsKey(room.getCode())) {
            throw new MatchAlreadyRunningException(room.getCode());
        }

        GameSession session = new GameSession(
                room, participantUsernames, gameEngine, roomBroadcaster, this::onSessionEnded);
        activeSessions.put(room.getCode(), session);
        session.start(); // non-blocking: just schedules the first tick and returns immediately
    }

    public synchronized void stopMatch(String roomCode) {
        GameSession session = activeSessions.get(roomCode);
        if (session == null) {
            throw new MatchNotRunningException(roomCode);
        }
        session.stop(); // triggers onSessionEnded (below) synchronously, which removes it from the map
    }

    /**
     * Called by a GameSession itself, exactly once, the instant it stops - whether
     * that was a host ending the match early or the duration timer expiring on its
     * own (see GameSession.stop()'s javadoc). Removing the session from the map AND
     * kicking off Sprint 6's persistence step both happen from this one spot so
     * neither can be missed or double-run.
     */
    private synchronized void onSessionEnded(GameSession session) {
        activeSessions.remove(session.getRoomCode());
        matchRecordService.persistMatchRecords(session);
    }

    public boolean isRunning(String roomCode) {
        return activeSessions.containsKey(roomCode);
    }

    /** Used by RoomWebSocketController to find the right room's arena when a fire event comes in. */
    public GameSession getSession(String roomCode) {
        return activeSessions.get(roomCode);
    }

    /**
     * Sprint 9 (admin dashboard): every currently-running match, for a live system
     * snapshot. Returns a read-only view backed directly by the ConcurrentHashMap -
     * same safe-to-iterate-while-changing pattern RoomArena already uses for its
     * targets, so a room starting/stopping while the admin dashboard is mid-read
     * can never throw a ConcurrentModificationException.
     */
    public Collection<GameSession> getAllActiveSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }
}
