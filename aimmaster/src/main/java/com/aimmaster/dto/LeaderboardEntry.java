package com.aimmaster.dto;

public record LeaderboardEntry(
        Integer playerId,
        String username,
        Integer score
) {
}