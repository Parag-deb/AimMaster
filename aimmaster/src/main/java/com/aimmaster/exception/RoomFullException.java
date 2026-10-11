package com.aimmaster.exception;

/** Thrown when a player tries to join a room that has already reached maxPlayers. */
public class RoomFullException extends RuntimeException {
    public RoomFullException(String code) {
        super("Room '" + code + "' is full");
    }
}
