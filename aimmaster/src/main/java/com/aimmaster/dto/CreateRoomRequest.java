package com.aimmaster.dto;

import com.aimmaster.model.TargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "Room code is required")
        @Size(min = 3, max = 10, message = "Code must be between 3 and 10 characters")
        String code,

        @NotNull(message = "Difficulty is required")
        TargetType difficulty,

        @Min(value = 10, message = "Duration must be at least 10 seconds")
        @Max(value = 3600, message = "Duration must be at most 3600 seconds")
        int durationSeconds,

        @Min(value = 1, message = "Max players must be at least 1")
        @Max(value = 100, message = "Max players must be at most 100")
        int maxPlayers
) {
}
