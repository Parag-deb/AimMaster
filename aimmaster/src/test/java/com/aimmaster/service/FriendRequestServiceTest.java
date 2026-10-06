package com.aimmaster.service;

import com.aimmaster.entity.Friend;
import com.aimmaster.entity.FriendRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.repository.FriendRepository;
import com.aimmaster.repository.FriendRequestRepository;
import com.aimmaster.repository.PlayerRepository;
import com.aimmaster.config.WebSocketSessionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendRequestServiceTest {

    private FriendRequestRepository requestRepository;
    private FriendRepository friendRepository;
    private PlayerRepository playerRepository;
    private WebSocketSessionTracker sessionTracker;
    private FriendRequestService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(FriendRequestRepository.class);
        friendRepository = mock(FriendRepository.class);
        playerRepository = mock(PlayerRepository.class);
        sessionTracker = mock(WebSocketSessionTracker.class);
        service = new FriendRequestService(requestRepository, friendRepository, playerRepository, sessionTracker);
    }

    @Test
    void sendRequest_createsPendingRequest() throws Exception {
        Player sender = new Player("alice", "pw");
        Player recipient = new Player("bob", "pw");
        java.lang.reflect.Field idField = Player.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(sender, 1);
        idField.set(recipient, 2);
        when(playerRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(playerRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(requestRepository.findBySenderUsernameAndRecipientUsernameAndStatus("alice", "bob", FriendRequest.Status.PENDING))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any(FriendRequest.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.sendRequest("alice", "bob");
        assertEquals("alice", response.senderUsername());
        assertEquals("bob", response.recipientUsername());
        assertEquals(FriendRequest.Status.PENDING, response.status());
    }

    @Test
    void sendRequest_toSelf_throwsException() {
        assertThrows(com.aimmaster.exception.BadRequestException.class,
                () -> service.sendRequest("alice", "alice"));
    }

    @Test
    void acceptRequest_createsFriendship() throws Exception {
        Player alice = new Player("alice", "pw");
        Player bob = new Player("bob", "pw");
        java.lang.reflect.Field idField = Player.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(alice, 1);
        idField.set(bob, 2);
        FriendRequest request = new FriendRequest(alice, bob);
        request.setStatus(FriendRequest.Status.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(playerRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendRepository.existsByPlayer1UsernameAndPlayer2Username("alice", "bob")).thenReturn(false);
        when(friendRepository.save(any(Friend.class))).thenAnswer(i -> i.getArgument(0));
        when(requestRepository.save(any(FriendRequest.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.acceptRequest("bob", 1L);
        assertEquals(FriendRequest.Status.ACCEPTED, response.status());
    }

    @Test
    void declineRequest_marksDeclined() {
        Player alice = new Player("alice", "pw");
        Player bob = new Player("bob", "pw");
        FriendRequest request = new FriendRequest(alice, bob);
        request.setStatus(FriendRequest.Status.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(FriendRequest.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.declineRequest("bob", 1L);
        assertEquals(FriendRequest.Status.DECLINED, response.status());
    }
}
