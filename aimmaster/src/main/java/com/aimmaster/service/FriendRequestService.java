package com.aimmaster.service;

import com.aimmaster.dto.FriendRequestResponse;
import com.aimmaster.entity.Friend;
import com.aimmaster.entity.FriendRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.BadRequestException;
import com.aimmaster.exception.NotFoundException;
import com.aimmaster.repository.FriendRepository;
import com.aimmaster.repository.FriendRequestRepository;
import com.aimmaster.repository.PlayerRepository;
import com.aimmaster.config.WebSocketSessionTracker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendRequestService {

    private final FriendRequestRepository requestRepository;
    private final FriendRepository friendRepository;
    private final PlayerRepository playerRepository;
    private final WebSocketSessionTracker sessionTracker;

    public FriendRequestService(FriendRequestRepository requestRepository,
                                FriendRepository friendRepository,
                                PlayerRepository playerRepository,
                                WebSocketSessionTracker sessionTracker) {
        this.requestRepository = requestRepository;
        this.friendRepository = friendRepository;
        this.playerRepository = playerRepository;
        this.sessionTracker = sessionTracker;
    }

    public FriendRequestResponse sendRequest(String senderUsername, String recipientUsername) {
        if (senderUsername.equals(recipientUsername)) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        Player sender = playerRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new NotFoundException("Sender not found"));
        Player recipient = playerRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));

        String a = sender.getUsername();
        String b = recipient.getUsername();
        if (a.compareTo(b) > 0) {
            String tmp = a; a = b; b = tmp;
        }

        if (friendRepository.existsByPlayer1UsernameAndPlayer2Username(a, b)) {
            throw new BadRequestException("Already friends with " + recipientUsername);
        }

        requestRepository.findBySenderUsernameAndRecipientUsernameAndStatus(
                senderUsername, recipientUsername, FriendRequest.Status.PENDING
        ).ifPresent(r -> {
            throw new BadRequestException("Friend request already pending to " + recipientUsername);
        });

        FriendRequest request = new FriendRequest(sender, recipient);
        FriendRequest saved = requestRepository.save(request);
        return toResponse(saved);
    }

    public FriendRequestResponse acceptRequest(String recipientUsername, Long requestId) {
        FriendRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (!request.getRecipient().getUsername().equals(recipientUsername)) {
            throw new BadRequestException("Not authorized to accept this request");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        request.setStatus(FriendRequest.Status.ACCEPTED);
        FriendRequest saved = requestRepository.save(request);

        Player p1 = request.getSender();
        Player p2 = request.getRecipient();
        String a = p1.getUsername();
        String b = p2.getUsername();
        if (a.compareTo(b) > 0) {
            String tmp = a; a = b; b = tmp;
        }

        friendRepository.save(new Friend(p1, p2));
        return toResponse(saved);
    }

    public FriendRequestResponse declineRequest(String recipientUsername, Long requestId) {
        FriendRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (!request.getRecipient().getUsername().equals(recipientUsername)) {
            throw new BadRequestException("Not authorized to decline this request");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        request.setStatus(FriendRequest.Status.DECLINED);
        FriendRequest saved = requestRepository.save(request);
        return toResponse(saved);
    }

    public void cancelRequest(String senderUsername, Long requestId) {
        FriendRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (!request.getSender().getUsername().equals(senderUsername)) {
            throw new BadRequestException("Not authorized to cancel this request");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        request.setStatus(FriendRequest.Status.CANCELLED);
        requestRepository.save(request);
    }

    public List<FriendRequestResponse> getReceivedRequests(String username) {
        List<FriendRequest> requests = requestRepository.findByRecipientUsername(username);
        List<FriendRequestResponse> result = new ArrayList<>();
        for (FriendRequest r : requests) {
            if (r.getStatus() == FriendRequest.Status.PENDING) {
                result.add(toResponse(r));
            }
        }
        return result;
    }

    public List<FriendRequestResponse> getSentRequests(String username) {
        List<FriendRequest> requests = requestRepository.findBySenderUsername(username);
        List<FriendRequestResponse> result = new ArrayList<>();
        for (FriendRequest r : requests) {
            if (r.getStatus() == FriendRequest.Status.PENDING) {
                result.add(toResponse(r));
            }
        }
        return result;
    }

    public boolean isOnline(String username) {
        return playerRepository.findByUsername(username)
                .map(p -> sessionTracker.getAllSessionsForPlayer(p.getId()).size() > 0)
                .orElse(false);
    }

    public boolean isInGame(String username) {
        return playerRepository.findByUsername(username)
                .map(p -> sessionTracker.getRoomsForPlayer(p.getId()).size() > 0)
                .orElse(false);
    }

    private FriendRequestResponse toResponse(FriendRequest request) {
        return new FriendRequestResponse(
                request.getId(),
                request.getSender().getUsername(),
                request.getRecipient().getUsername(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
