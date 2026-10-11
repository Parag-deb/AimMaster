package com.aimmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for PUT /api/players/me — update profile fields. */
public record UpdatePlayerRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 32, message = "Username must be 3-32 characters")
        String username,

        String theme
) {
}
