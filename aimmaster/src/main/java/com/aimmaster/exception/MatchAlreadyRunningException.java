package com.aimmaster.exception;

/** Thrown when someone tries to start a match in a room that already has one running. */
public class MatchAlreadyRunningException extends RuntimeException {
    public MatchAlreadyRunningException(String roomCode) {
        super("A match is already running in room '" + roomCode + "'");
    }
}
