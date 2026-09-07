package com.aimmaster.dto;

public record AuthResponse(
        String token,
        String username,
        Integer playerId
) {
}
