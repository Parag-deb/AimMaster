package com.aimmaster.dto;

/**
 * Sprint 3's throwaway "does the pipe work" payload - a client sends one of these to
 * /app/room/{roomId}/test, the server stamps who-sent-it and re-broadcasts it to
 * /topic/room/{roomId}. Sprint 4+ will introduce real payloads (target positions,
 * fire events) - this class is not meant to survive into later sprints.
 */
public class WsTestMessage {

    private String content;
    private String senderUsername; // filled in by the server, ignored if the client sets it
    private long serverTimestamp;  // filled in by the server

    public WsTestMessage() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(long serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}
