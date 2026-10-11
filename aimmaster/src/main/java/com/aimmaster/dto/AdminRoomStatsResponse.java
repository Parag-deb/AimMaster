package com.aimmaster.dto;

import com.aimmaster.model.Difficulty;

/** Sprint 9 (admin dashboard): one row of the live per-room snapshot (FR11). */
public class AdminRoomStatsResponse {

    private final String roomCode;
    private final Difficulty difficulty;
    private final boolean matchRunning;
    private final double elapsedSeconds;
    private final int targetCount;
    private final int connectedPlayers;
    private final int spectators;

    public AdminRoomStatsResponse(String roomCode, Difficulty difficulty, boolean matchRunning,
                                   double elapsedSeconds, int targetCount, int connectedPlayers, int spectators) {
        this.roomCode = roomCode;
        this.difficulty = difficulty;
        this.matchRunning = matchRunning;
        this.elapsedSeconds = elapsedSeconds;
        this.targetCount = targetCount;
        this.connectedPlayers = connectedPlayers;
        this.spectators = spectators;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public boolean isMatchRunning() {
        return matchRunning;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getConnectedPlayers() {
        return connectedPlayers;
    }

    public int getSpectators() {
        return spectators;
    }
}
