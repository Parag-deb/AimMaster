package com.aimmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the client sends to POST /api/auth/register.
 * We NEVER accept or expose the entity.Player class directly over REST - DTOs are the
 * only thing that crosses the network boundary, so we can change the DB shape later
 * without breaking the API contract.
 */
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 32, message = "username must be between 3 and 32 characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be at least 6 characters")
    private String password;

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
