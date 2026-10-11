package com.aimmaster.exception;

/** Thrown when someone tries to register a username that's already taken. */
public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
