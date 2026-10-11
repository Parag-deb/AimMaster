package com.aimmaster.model;

/**
 * How fast/aggressive a room's targets are. A Room host picks one of these when
 * creating a room. Each level carries its own starting spawn rate and target speed;
 * GameEngine (Sprint 4) reads these as the STARTING point and then scales both up
 * further the longer a match runs (FR15 - difficulty increases over time too).
 */
public enum Difficulty {

    EASY(2000, 80.0),
    MEDIUM(1400, 130.0),
    HARD(900, 190.0);

    private final int baseSpawnIntervalMs;
    private final double baseSpeed; // pixels per second

    Difficulty(int baseSpawnIntervalMs, double baseSpeed) {
        this.baseSpawnIntervalMs = baseSpawnIntervalMs;
        this.baseSpeed = baseSpeed;
    }

    public int getBaseSpawnIntervalMs() {
        return baseSpawnIntervalMs;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }
}
