package com.aimmaster.dto;

/** What we send back from the start/stop match endpoints. */
public class MatchStatusResponse {

    private final String roomCode;
    private final String status; // "RUNNING" or "STOPPED"

    public MatchStatusResponse(String roomCode, String status) {
        this.roomCode = roomCode;
        this.status = status;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getStatus() {
        return status;
    }
}
