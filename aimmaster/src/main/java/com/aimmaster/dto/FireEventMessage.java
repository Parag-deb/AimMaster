package com.aimmaster.dto;

/**
 * What a client sends to /app/room/{roomId}/fire when the thumb-fold gesture fires
 * (FR5): just which target they believe they hit. The server (HitResolver) is the
 * only thing that decides whether that actually counts (FR13) - this message is a
 * claim, not a fact.
 */
public class FireEventMessage {

    private String targetId;

    public FireEventMessage() {
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
