package com.aimmaster.dto;

import com.aimmaster.entity.Friend;

import java.time.Instant;

public record FriendResponse(
        Long id,
        String username,
        String otherUsername,
        Instant createdAt
) {
}
