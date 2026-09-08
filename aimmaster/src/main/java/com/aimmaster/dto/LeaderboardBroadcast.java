package com.aimmaster.dto;

import java.util.List;

public record LeaderboardBroadcast(
        List<LeaderboardEntry> standings,
        long timestamp
) {
}