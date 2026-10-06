package com.aimmaster.dto;

import com.aimmaster.entity.FriendRequest;

import java.time.Instant;

public record FriendRequestResponse(
        Long id,
        String senderUsername,
        String recipientUsername,
        FriendRequest.Status status,
        Instant createdAt,
        Instant updatedAt
) {
}
