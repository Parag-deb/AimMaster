package com.aimmaster.dto;

import java.time.Instant;

public record MatchRecordResponse(
        Long id,
        Integer playerId,
        Integer roomId,
        Integer score,
        Double accuracy,
        Integer shotsFired,
        Integer hits,
        Instant timestamp
) {
}