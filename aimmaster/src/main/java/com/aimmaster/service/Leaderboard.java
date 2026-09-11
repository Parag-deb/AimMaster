package com.aimmaster.service;

import com.aimmaster.dto.LeaderboardEntry;
import com.aimmaster.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Per-room live leaderboard service.
 *
 * Responsibility:
 * - Tracks scores for players in a room during an active match.
 * - Provides thread-safe score increments and leaderboard snapshots.
 * - Does NOT persist to database — that happens at match end (Sprint 6).
 * - Kept in-memory for low-latency updates during the ~100ms game loop.
 */
@Service
public class Leaderboard {

    private static final Logger log = LoggerFactory.getLogger(Leaderboard.class);

    /**
     * Room ID -> (Player ID -> Score).
     * Uses ConcurrentHashMap for thread-safe access from WebSocket handlers.
     */
    private final Map<Integer, Map<Integer, AtomicInteger>> roomScores = new ConcurrentHashMap<>();

    /**
     * Room ID -> (Player ID -> Username) for display in leaderboard broadcasts.
     */
    private final Map<Integer, Map<Integer, String>> roomPlayerNames = new ConcurrentHashMap<>();

    /**
     * Records a player joining a room so their name is available for leaderboard display.
     */
    public void registerPlayer(int roomId, Player player) {
        roomScores.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        roomPlayerNames.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        roomPlayerNames.get(roomId).putIfAbsent(player.getId(), player.getUsername());
        // Initialize score to 0 if not present
        roomScores.get(roomId).putIfAbsent(player.getId(), new AtomicInteger(0));
    }

    /**
     * Removes a player from the room's scoreboard.
     */
    public void unregisterPlayer(int roomId, int playerId) {
        Map<Integer, AtomicInteger> scores = roomScores.get(roomId);
        if (scores != null) {
            scores.remove(playerId);
        }
        Map<Integer, String> names = roomPlayerNames.get(roomId);
        if (names != null) {
            names.remove(playerId);
        }
    }

    /**
     * Increments the player's score by the given points (default 10 per hit).
     * Thread-safe: uses AtomicInteger for the score value.
     *
     * @param roomId   the room where the hit occurred
     * @param playerId the player who scored
     * @param points   points to add (typically 10)
     * @return the player's new total score
     */
    public int addScore(int roomId, int playerId, int points) {
        Map<Integer, AtomicInteger> scores = roomScores.get(roomId);
        if (scores == null) {
            log.warn("Score increment for unknown room {}", roomId);
            return 0;
        }
        AtomicInteger scoreRef = scores.computeIfAbsent(playerId, k -> new AtomicInteger(0));
        int newScore = scoreRef.addAndGet(points);
        log.debug("Player {} score in room {} is now {}", playerId, roomId, newScore);
        return newScore;
    }

    /**
     * Gets the current score for a player in a room.
     */
    public int getScore(int roomId, int playerId) {
        Map<Integer, AtomicInteger> scores = roomScores.get(roomId);
        if (scores == null) {
            return 0;
        }
        AtomicInteger scoreRef = scores.get(playerId);
        return scoreRef != null ? scoreRef.get() : 0;
    }

    /**
     * Builds a sorted leaderboard snapshot for the given room.
     * Sorted by score descending (highest first).
     *
     * @param roomId the room to get standings for
     * @return list of LeaderboardEntry sorted by score (highest first)
     */
    public List<LeaderboardEntry> getStandings(int roomId) {
        Map<Integer, AtomicInteger> scores = roomScores.get(roomId);
        Map<Integer, String> names = roomPlayerNames.get(roomId);

        if (scores == null || scores.isEmpty()) {
            return List.of();
        }

        return scores.entrySet().stream()
                .map(entry -> {
                    int playerId = entry.getKey();
                    int score = entry.getValue().get();
                    String username = names != null ? names.getOrDefault(playerId, "Player" + playerId)
                            : "Player" + playerId;
                    return new LeaderboardEntry(playerId, username, score);
                })
                .sorted(Comparator.comparingInt(LeaderboardEntry::score).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Clears all scores and player names for a room (called when match ends or room is cleaned up).
     */
    public void clearRoom(int roomId) {
        roomScores.remove(roomId);
        roomPlayerNames.remove(roomId);
        log.debug("Cleared leaderboard for room {}", roomId);
    }
}