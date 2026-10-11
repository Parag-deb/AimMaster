package com.aimmaster.dto;

import java.util.List;

/**
 * Broadcast every ~100ms to /topic/room/{roomId} while a match is running (FR6:
 * everyone in the room sees the same moving targets, live). Kept intentionally small -
 * just enough for the frontend to redraw the arena each tick.
 */
public class ArenaStateMessage {

    private final String roomCode;
    private final double elapsedSeconds;
    private final List<TargetSnapshot> targets;

    public ArenaStateMessage(String roomCode, double elapsedSeconds, List<TargetSnapshot> targets) {
        this.roomCode = roomCode;
        this.elapsedSeconds = elapsedSeconds;
        this.targets = targets;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public List<TargetSnapshot> getTargets() {
        return targets;
    }
}
