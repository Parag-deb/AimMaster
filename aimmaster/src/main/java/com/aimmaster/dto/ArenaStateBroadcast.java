package com.aimmaster.dto;

import java.util.List;

public record ArenaStateBroadcast(
        Integer roomId,
        List<TargetState> targets,
        long elapsedMs,
        boolean running
) {
}