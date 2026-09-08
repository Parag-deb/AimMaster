package com.aimmaster.dto;

public record TestMessageBroadcast(
        String from,
        String content,
        long timestamp
) {
}