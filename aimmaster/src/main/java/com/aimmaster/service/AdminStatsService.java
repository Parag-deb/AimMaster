package com.aimmaster.service;

import com.aimmaster.dto.AdminRoomStatsResponse;
import com.aimmaster.dto.AdminSystemStatsResponse;
import com.aimmaster.entity.Room;
import com.aimmaster.model.Difficulty;
import com.aimmaster.repository.RoomRepository;
import com.aimmaster.websocket.RoomPresenceListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Sprint 9 (admin dashboard, FR11): builds one combined snapshot of every room that
 * currently has ANY activity - a running match, OR just people sitting connected in
 * the lobby before the host has started one. That's why this pulls from two sources
 * rather than just GameSessionManager: a room with three players waiting for the
 * host to click "start" is still worth showing an admin, even with matchRunning=false.
 *
 * Kept as its own service (not stuffed into AdminController) so the controller stays
 * thin, per the project's layering rule - controllers call one service method and
 * return the result.
 */
@Service
public class AdminStatsService {

    private final GameSessionManager gameSessionManager;
    private final RoomPresenceListener roomPresenceListener;
    private final RoomRepository roomRepository;

    public AdminStatsService(GameSessionManager gameSessionManager, RoomPresenceListener roomPresenceListener,
                              RoomRepository roomRepository) {
        this.gameSessionManager = gameSessionManager;
        this.roomPresenceListener = roomPresenceListener;
        this.roomRepository = roomRepository;
    }

    public AdminSystemStatsResponse getSystemStats() {
        Map<String, Integer> playerCounts = roomPresenceListener.getConnectedPlayerCountsByRoom();
        Map<String, Integer> spectatorCounts = roomPresenceListener.getSpectatorCountsByRoom();

        // TreeSet -> alphabetical room codes, so the dashboard table has a stable,
        // predictable row order instead of jumping around between refreshes.
        TreeSet<String> roomCodes = new TreeSet<>();
        roomCodes.addAll(playerCounts.keySet());
        roomCodes.addAll(spectatorCounts.keySet());
        gameSessionManager.getAllActiveSessions().forEach(session -> roomCodes.add(session.getRoomCode()));

        List<AdminRoomStatsResponse> rows = new ArrayList<>();
        for (String code : roomCodes) {
            rows.add(buildRoomStats(code, playerCounts, spectatorCounts));
        }

        return new AdminSystemStatsResponse(
                gameSessionManager.getAllActiveSessions().size(),
                Thread.activeCount(),
                rows
        );
    }

    private AdminRoomStatsResponse buildRoomStats(String code, Map<String, Integer> playerCounts,
                                                    Map<String, Integer> spectatorCounts) {
        GameSession session = gameSessionManager.getSession(code);

        Difficulty difficulty = session != null
                ? session.getDifficulty()
                : roomRepository.findByCode(code).map(Room::getDifficulty).orElse(null);

        return new AdminRoomStatsResponse(
                code,
                difficulty,
                session != null,
                session != null ? session.getElapsedSeconds() : 0.0,
                session != null ? session.getArena().size() : 0,
                playerCounts.getOrDefault(code, 0),
                spectatorCounts.getOrDefault(code, 0)
        );
    }
}
