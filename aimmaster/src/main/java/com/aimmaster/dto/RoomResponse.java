package com.aimmaster.dto;

import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.model.Difficulty;

import java.util.List;

/**
 * What we send back for a room - built from entity.Room but never exposing the entity
 * itself (e.g. we don't want to leak every joined player's password hash by accident).
 */
public class RoomResponse {

    private final String code;
    private final String hostUsername;
    private final Difficulty difficulty;
    private final int durationSeconds;
    private final int maxPlayers;
    private final List<String> playerUsernames;

    public RoomResponse(String code, String hostUsername, Difficulty difficulty,
                         int durationSeconds, int maxPlayers, List<String> playerUsernames) {
        this.code = code;
        this.hostUsername = hostUsername;
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
        this.maxPlayers = maxPlayers;
        this.playerUsernames = playerUsernames;
    }

    /** Small factory so controllers/services don't repeat this mapping logic (anti-duplication). */
    public static RoomResponse from(Room room) {
        List<String> usernames = room.getPlayers().stream()
                .map(Player::getUsername)
                .toList();
        return new RoomResponse(
                room.getCode(),
                room.getHost().getUsername(),
                room.getDifficulty(),
                room.getDurationSeconds(),
                room.getMaxPlayers(),
                usernames
        );
    }

    public String getCode() {
        return code;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public List<String> getPlayerUsernames() {
        return playerUsernames;
    }
}
