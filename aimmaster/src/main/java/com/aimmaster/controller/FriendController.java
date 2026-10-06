package com.aimmaster.controller;

import com.aimmaster.dto.FriendRequestResponse;
import com.aimmaster.dto.FriendResponse;
import com.aimmaster.dto.UserStatusResponse;
import com.aimmaster.service.FriendRequestService;
import com.aimmaster.service.FriendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.aimmaster.entity.Player;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final FriendRequestService friendRequestService;

    public FriendController(FriendService friendService, FriendRequestService friendRequestService) {
        this.friendService = friendService;
        this.friendRequestService = friendRequestService;
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends() {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(friendService.getFriends(player.getUsername()));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> removeFriend(@PathVariable String username) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        friendService.removeFriend(player.getUsername(), username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request/{recipientUsername}")
    public ResponseEntity<FriendRequestResponse> sendRequest(@PathVariable String recipientUsername) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                friendRequestService.sendRequest(player.getUsername(), recipientUsername)
        );
    }

    @GetMapping("/requests/received")
    public ResponseEntity<List<FriendRequestResponse>> getReceivedRequests() {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(friendRequestService.getReceivedRequests(player.getUsername()));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<List<FriendRequestResponse>> getSentRequests() {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(friendRequestService.getSentRequests(player.getUsername()));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> acceptRequest(@PathVariable Long requestId) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(friendRequestService.acceptRequest(player.getUsername(), requestId));
    }

    @PostMapping("/requests/{requestId}/decline")
    public ResponseEntity<FriendRequestResponse> declineRequest(@PathVariable Long requestId) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(friendRequestService.declineRequest(player.getUsername(), requestId));
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long requestId) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        friendRequestService.cancelRequest(player.getUsername(), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, UserStatusResponse>> getFriendsStatus() {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<FriendResponse> friends = friendService.getFriends(player.getUsername());
        Map<String, UserStatusResponse> statuses = new java.util.HashMap<>();
        for (FriendResponse friend : friends) {
            String other = friend.otherUsername();
            statuses.put(other, new UserStatusResponse(
                    other,
                    friendRequestService.isOnline(other),
                    friendRequestService.isInGame(other)
            ));
        }
        return ResponseEntity.ok(statuses);
    }
}
