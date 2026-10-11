package com.aimmaster.exception;

/** Thrown when someone tries to stop a match in a room that doesn't have one running. */
public class MatchNotRunningException extends RuntimeException {
    public MatchNotRunningException(String roomCode) {
        super("No match is currently running in room '" + roomCode + "'");
    }
}
