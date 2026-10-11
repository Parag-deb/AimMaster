package com.aimmaster.dto;

import com.aimmaster.model.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** What the host sends to POST /api/rooms to create a new room (FR9). */
public class CreateRoomRequest {

    @NotNull(message = "difficulty is required")
    private Difficulty difficulty;

    @Min(value = 30, message = "duration must be at least 30 seconds")
    @Max(value = 3600, message = "duration must be at most 1 hour")
    private int durationSeconds;

    @Min(value = 1, message = "a room needs at least 1 player slot")
    @Max(value = 6, message = "rooms are capped at 6 players (see NFR: Scalability)")
    private int maxPlayers;

    public CreateRoomRequest() {
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
