package com.aimmaster.dto;

/**
 * Broadcast to /topic/room/{roomId} the instant a shot is confirmed as a real hit
 * (FR7) - fixed payload shape, locked here in Sprint 5, per
 * AimMaster_02_Sprint_Plan.md: "{ targetId, hitByPlayerId, newScore, timestamp }".
 *
 * One deliberate, documented naming choice: the sprint plan's "hitByPlayerId" is sent
 * here as a username (hitByUsername) instead of a numeric database id, to stay
 * consistent with every other DTO in this project (RoomResponse.hostUsername,
 * WsTestMessage.senderUsername, etc. all identify a player by username, never by
 * their internal Player.id) - the frontend never has to juggle two different kinds
 * of player identifier.
 */
public class HitResultMessage {

    private final String targetId;
    private final String hitByUsername;
    private final int newScore;
    private final long timestamp;

    public HitResultMessage(String targetId, String hitByUsername, int newScore, long timestamp) {
        this.targetId = targetId;
        this.hitByUsername = hitByUsername;
        this.newScore = newScore;
        this.timestamp = timestamp;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getHitByUsername() {
        return hitByUsername;
    }

    public int getNewScore() {
        return newScore;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
