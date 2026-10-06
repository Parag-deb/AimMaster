package com.aimmaster.dto;

public record UserStatusResponse(
        String username,
        boolean online,
        boolean inGame
) {
}
