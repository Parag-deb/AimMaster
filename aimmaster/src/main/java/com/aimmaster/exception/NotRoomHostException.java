package com.aimmaster.exception;

/** Thrown when a non-host player tries to do something only the room's host is allowed to do. */
public class NotRoomHostException extends RuntimeException {
    public NotRoomHostException(String roomCode) {
        super("Only the host of room '" + roomCode + "' can do that");
    }
}
