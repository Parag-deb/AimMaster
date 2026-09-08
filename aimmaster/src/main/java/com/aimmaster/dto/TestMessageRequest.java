package com.aimmaster.dto;

import jakarta.validation.constraints.NotNull;

public record TestMessageRequest(
        @NotNull(message = "Content is required")
        String content
) {
}