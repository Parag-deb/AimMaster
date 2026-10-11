package com.aimmaster.dto;

import com.aimmaster.entity.MatchRecord;

import java.time.Instant;

/** One row of a player's match history. */
public class MatchHistoryEntry {

    private final String roomCode;
    private final int score;
    private final double accuracyPercent;
    private final Instant timestamp;

    public MatchHistoryEntry(String roomCode, int score, double accuracyPercent, Instant timestamp) {
        this.roomCode = roomCode;
        this.score = score;
        this.accuracyPercent = accuracyPercent;
        this.timestamp = timestamp;
    }

    /** Converts the entity's stored 0.0-1.0 fraction into a friendlier 0-100 percentage for display. */
    public static MatchHistoryEntry from(MatchRecord record) {
        return new MatchHistoryEntry(
                record.getRoom().getCode(),
                record.getScore(),
                record.getAccuracy() * 100.0,
                record.getTimestamp()
        );
    }

    public String getRoomCode() {
        return roomCode;
    }

    public int getScore() {
        return score;
    }

    public double getAccuracyPercent() {
        return accuracyPercent;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
