package com.aimmaster.dto;

/** What we send back after a successful register/login: a JWT the client must attach to future requests. */
public class AuthResponse {

    private final String token;
    private final String username;

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }
}
