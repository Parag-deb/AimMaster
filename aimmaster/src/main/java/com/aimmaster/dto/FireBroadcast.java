package com.aimmaster.dto;

public record FireBroadcast(
        Integer targetId,
        Integer hitByPlayerId,
        Integer newScore,
        long timestamp
) {
}