package com.aimmaster.service;

import com.aimmaster.dto.FriendResponse;
import com.aimmaster.entity.Friend;
import com.aimmaster.entity.Player;
import com.aimmaster.repository.FriendRepository;
import com.aimmaster.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendServiceTest {

    private FriendRepository friendRepository;
    private PlayerRepository playerRepository;
    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendRepository = mock(FriendRepository.class);
        playerRepository = mock(PlayerRepository.class);
        friendService = new FriendService(friendRepository, playerRepository);
    }

    @Test
    void getFriends_returnsFriends() {
        Player alice = new Player("alice", "pw");
        Player bob = new Player("bob", "pw");
        Friend friendship = new Friend(alice, bob);
        when(playerRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendRepository.findByInvolvingUsername("alice")).thenReturn(List.of(friendship));

        List<FriendResponse> result = friendService.getFriends("alice");
        assertEquals(1, result.size());
        assertEquals("bob", result.get(0).otherUsername());
    }

    @Test
    void removeFriend_deletesFriendship() {
        Player alice = new Player("alice", "pw");
        Player bob = new Player("bob", "pw");
        Friend friendship = new Friend(alice, bob);
        when(playerRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(playerRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendRepository.findByPlayer1UsernameAndPlayer2Username("alice", "bob"))
                .thenReturn(Optional.of(friendship));
        doNothing().when(friendRepository).delete(any(Friend.class));

        friendService.removeFriend("alice", "bob");
        verify(friendRepository).delete(friendship);
    }
}
