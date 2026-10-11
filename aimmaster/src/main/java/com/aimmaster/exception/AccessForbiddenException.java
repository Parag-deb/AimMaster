package com.aimmaster.exception;

/**
 * A general-purpose "you're logged in, but not allowed to do THIS" exception (403).
 * NotRoomHostException stays separate (its message is specific and already in use
 * since Sprint 4) - this one exists so Sprint 6+ don't each invent their own
 * narrowly-named 403 exception for every new permission check.
 */
public class AccessForbiddenException extends RuntimeException {
    public AccessForbiddenException(String message) {
        super(message);
    }
}
