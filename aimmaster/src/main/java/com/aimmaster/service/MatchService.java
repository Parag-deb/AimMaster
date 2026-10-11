package com.aimmaster.service;

import com.aimmaster.dto.LiveMatchStateResponse;
import com.aimmaster.dto.MatchStatusResponse;
import com.aimmaster.dto.TargetSnapshot;
import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.exception.NotRoomHostException;
import com.aimmaster.exception.RoomNotFoundException;
import com.aimmaster.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FR10: host starts/ends a match manually. This is deliberately its OWN service
 * (not folded into RoomService) - RoomService owns room CRUD (create/join),
 * MatchService owns match LIFECYCLE (start/stop the live game). Two different
 * responsibilities, two classes - see AimMaster_03_Agent_Instructions.md Section 2:
 * "no god-classes."
 */
@Service
public class MatchService {

    private final RoomRepository roomRepository;
    private final GameSessionManager gameSessionManager;

    public MatchService(RoomRepository roomRepository, GameSessionManager gameSessionManager) {
        this.roomRepository = roomRepository;
        this.gameSessionManager = gameSessionManager;
    }

    /**
     * "@Transactional" matters here for a Sprint 6 reason, not just a Sprint 4 one:
     * room.getPlayers() is a lazily-loaded collection (see entity.Room's Sprint 2
     * javadoc). We read it into a plain Set<String> RIGHT HERE, while the database
     * session this method opened is still active, and hand GameSession that plain
     * snapshot instead of the live entity - GameSession's thread runs for minutes,
     * long after this method (and its database session) has returned, so it must
     * never touch a lazy JPA collection itself.
     */
    @Transactional
    public MatchStatusResponse startMatch(String roomCode, String requestingUsername) {
        Room room = loadRoomAndVerifyHost(roomCode, requestingUsername);

        Set<String> participantUsernames = room.getPlayers().stream()
                .map(Player::getUsername)
                .collect(Collectors.toSet());

        gameSessionManager.startMatch(room, participantUsernames);
        return new MatchStatusResponse(roomCode, "RUNNING");
    }

    public MatchStatusResponse stopMatch(String roomCode, String requestingUsername) {
        loadRoomAndVerifyHost(roomCode, requestingUsername); // also confirms the room itself exists
        gameSessionManager.stopMatch(roomCode);
        return new MatchStatusResponse(roomCode, "STOPPED");
    }

    private Room loadRoomAndVerifyHost(String roomCode, String requestingUsername) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));

        if (!room.getHost().getUsername().equals(requestingUsername)) {
            throw new NotRoomHostException(roomCode);
        }
        return room;
    }

    /**
     * Sprint 9 (reconnect handling): what's happening in this room's match RIGHT
     * NOW, read straight from the live in-memory GameSession - not the database
     * (that's the historical /leaderboard endpoint's job, Sprint 6). No host check
     * and no room-membership check here on purpose: any authenticated player OR
     * spectator asking "what's the current state?" is always a safe, read-only
     * question to answer, the same way GET /leaderboard already has no host
     * restriction.
     */
    public LiveMatchStateResponse getLiveState(String roomCode) {
        GameSession session = gameSessionManager.getSession(roomCode);
        if (session == null) {
            return new LiveMatchStateResponse(false, 0.0, List.of(), List.of());
        }

        List<TargetSnapshot> targets = session.getArena().getTargets().stream()
                .map(TargetSnapshot::from)
                .toList();

        return new LiveMatchStateResponse(
                true, session.getElapsedSeconds(), targets, session.getLeaderboard().standings());
    }
}
