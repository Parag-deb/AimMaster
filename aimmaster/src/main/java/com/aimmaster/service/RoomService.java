package com.aimmaster.service;

import com.aimmaster.dto.CreateRoomRequest;
import com.aimmaster.dto.PlayerJoinedMessage;
import com.aimmaster.dto.RoomResponse;
import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.exception.RoomFullException;
import com.aimmaster.exception.RoomNotFoundException;
import com.aimmaster.repository.PlayerRepository;
import com.aimmaster.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Business logic for FR2/FR9 (create a room, join a room by code). Room-code
 * generation lives here (not on the entity) because "is this code already taken?"
 * requires asking the database - that's service-layer work, not the entity's job.
 */
@Service
public class RoomService {

    // No 0/O/1/I - easy for a host to read aloud or type without confusing similar characters.
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomBroadcaster roomBroadcaster;
    private final GameSessionManager gameSessionManager;

    public RoomService(RoomRepository roomRepository, PlayerRepository playerRepository,
                       RoomBroadcaster roomBroadcaster, GameSessionManager gameSessionManager) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.roomBroadcaster = roomBroadcaster;
        this.gameSessionManager = gameSessionManager;
    }

    @Transactional
    public RoomResponse createRoom(String hostUsername, CreateRoomRequest request) {
        Player host = loadPlayer(hostUsername);

        String code = generateUniqueCode();
        Room room = new Room(code, host, request.getDifficulty(), request.getDurationSeconds(), request.getMaxPlayers());
        roomRepository.save(room);

        return RoomResponse.from(room);
    }

    @Transactional
    public RoomResponse joinRoom(String username, String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RoomNotFoundException(code));

        if (room.isFull()) {
            throw new RoomFullException(code);
        }

        Player player = loadPlayer(username);
        room.addPlayer(player);
        roomRepository.save(room);

        // Broadcast a "player joined" notification to everyone in the room
        // (including the host, so they see who just joined).
        roomBroadcaster.broadcastToRoom(code, new PlayerJoinedMessage(username));

        // If a match is already running in this room, add the new player to the
        // live leaderboard so they appear with score 0 immediately (FR17).
        GameSession session = gameSessionManager.getSession(code);
        if (session != null && session.isActive()) {
            session.addPlayerToLeaderboard(username);
        }

        return RoomResponse.from(room);
    }

    /**
     * Sprint 9 (spectator mode): plain, read-only room info - no side effects at all.
     * Deliberately separate from joinRoom(): joining adds the caller to persisted
     * room_players membership and fails once the room is full (FR2's player cap);
     * a spectator should never consume a player slot or become a "member" just for
     * watching. This is also what makes it safe to call even on a completely full
     * room, or on a match already in progress.
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoom(String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RoomNotFoundException(code));
        return RoomResponse.from(room);
    }

    private Player loadPlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated username '" + username + "' has no matching player row"));
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
