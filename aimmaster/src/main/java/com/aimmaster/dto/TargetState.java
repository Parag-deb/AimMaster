package com.aimmaster.dto;

public record TargetState(
        int id,
        String type,
        double x,
        double y,
        double velocityX,
        double velocityY
) {
}