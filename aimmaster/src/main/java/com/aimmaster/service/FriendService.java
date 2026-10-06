package com.aimmaster.service;

import com.aimmaster.dto.FriendResponse;
import com.aimmaster.entity.Friend;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.BadRequestException;
import com.aimmaster.exception.NotFoundException;
import com.aimmaster.repository.FriendRepository;
import com.aimmaster.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final PlayerRepository playerRepository;

    public FriendService(FriendRepository friendRepository, PlayerRepository playerRepository) {
        this.friendRepository = friendRepository;
        this.playerRepository = playerRepository;
    }

    public List<FriendResponse> getFriends(String username) {
        List<Friend> friendships = friendRepository.findByInvolvingUsername(username);
        List<FriendResponse> result = new ArrayList<>();
        for (Friend f : friendships) {
            String other = f.otherUsername(username);
            if (other != null) {
                result.add(new FriendResponse(f.getId(), username, other, f.getCreatedAt()));
            }
        }
        return result;
    }

    public void removeFriend(String username, String friendUsername) {
        Player p1 = playerRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Player not found"));
        Player p2 = playerRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new NotFoundException("Friend not found"));

        String a = p1.getUsername();
        String b = p2.getUsername();
        if (a.compareTo(b) > 0) {
            String tmp = a; a = b; b = tmp;
        }

        Friend friend = friendRepository.findByPlayer1UsernameAndPlayer2Username(a, b)
                .orElseThrow(() -> new BadRequestException("Not friends with " + friendUsername));
        friendRepository.delete(friend);
    }
}
