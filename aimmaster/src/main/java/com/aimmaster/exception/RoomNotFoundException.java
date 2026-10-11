package com.aimmaster.exception;

/** Thrown when a room code doesn't match any existing room. */
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String code) {
        super("No room found with code '" + code + "'");
    }
}
