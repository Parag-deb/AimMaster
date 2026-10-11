package com.aimmaster.dto;

/**
 * Broadcast when a player joins a room. Sent to everyone currently subscribed
 * to that room's topic so the host (and other players) see a live "user joined"
 * notification on their screen. The "type" field distinguishes this from
 * PlayerLeftMessage (which also carries a username).
 */
public class PlayerJoinedMessage {

    private final String type = "PLAYER_JOINED";
    private final String username;
    private final String roomCode;

    public PlayerJoinedMessage(String username) {
        this.username = username;
        this.roomCode = null;
    }

    public PlayerJoinedMessage(String username, String roomCode) {
        this.username = username;
        this.roomCode = roomCode;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getRoomCode() {
        return roomCode;
    }
}