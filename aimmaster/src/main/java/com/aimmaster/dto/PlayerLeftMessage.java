package com.aimmaster.dto;

/**
 * FR16: broadcast to /topic/room/{roomId} when a player disconnects mid-match while
 * other players remain in the room, so their screens can show a friendly "so-and-so
 * left" notice instead of just silently missing someone with no explanation. Purely
 * informational - it changes nothing about scoring or the game loop.
 */
public class PlayerLeftMessage {

    private final String username;

    public PlayerLeftMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
