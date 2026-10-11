package com.aimmaster.dto;

/** One row of a room's leaderboard: a username and their current score. */
public class LeaderboardEntry {

    private final String username;
    private final int score;

    public LeaderboardEntry(String username, int score) {
        this.username = username;
        this.score = score;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }
}
