package com.aimmaster.entity;

import com.aimmaster.model.TargetType;
import jakarta.persistence.Embeddable;

@Embeddable
public class RoomSettings {
    private TargetType difficulty;
    private int durationSeconds;
    private int maxPlayers;

    protected RoomSettings() {
    }

    public RoomSettings(TargetType difficulty, int durationSeconds, int maxPlayers) {
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
        this.maxPlayers = maxPlayers;
    }

    public TargetType getDifficulty() {
        return difficulty;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
