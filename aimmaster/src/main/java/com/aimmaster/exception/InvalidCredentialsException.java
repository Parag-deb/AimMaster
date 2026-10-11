package com.aimmaster.exception;

/** Thrown when a login attempt's username/password don't match. Deliberately vague in its
 *  message (doesn't say WHICH field is wrong) so we don't help an attacker guess usernames. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
