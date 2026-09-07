package com.aimmaster.dto;

import com.aimmaster.entity.Room;
import com.aimmaster.model.TargetType;

import java.util.List;

public record RoomResponse(
        Integer id,
        String code,
        String hostUsername,
        TargetType difficulty,
        int durationSeconds,
        int maxPlayers,
        int playerCount
) {
    public static RoomResponse from(Room room, int playerCount) {
        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getHost().getUsername(),
                room.getSettings().getDifficulty(),
                room.getSettings().getDurationSeconds(),
                room.getSettings().getMaxPlayers(),
                playerCount
        );
    }
}
