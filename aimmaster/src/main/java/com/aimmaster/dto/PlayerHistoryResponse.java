package com.aimmaster.dto;

import java.util.List;

/** FR8: a player's full match history plus a couple of simple lifetime stats. */
public class PlayerHistoryResponse {

    private final String username;
    private final int totalMatches;
    private final double averageScore;
    private final double averageAccuracyPercent;
    private final List<MatchHistoryEntry> matches;

    public PlayerHistoryResponse(String username, int totalMatches, double averageScore,
                                  double averageAccuracyPercent, List<MatchHistoryEntry> matches) {
        this.username = username;
        this.totalMatches = totalMatches;
        this.averageScore = averageScore;
        this.averageAccuracyPercent = averageAccuracyPercent;
        this.matches = matches;
    }

    public String getUsername() {
        return username;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getAverageAccuracyPercent() {
        return averageAccuracyPercent;
    }

    public List<MatchHistoryEntry> getMatches() {
        return matches;
    }
}
