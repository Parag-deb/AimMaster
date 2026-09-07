package com.aimmaster.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank(message = "Room code is required")
        String code
) {
}
