package com.aimmaster.repository;

import com.aimmaster.entity.FriendRequest;
import com.aimmaster.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Handles the friend_requests table. Two read directions matter for the UI:
 *   - "requests I SENT" (shown on the sender's Friends page so they can cancel)
 *   - "requests I RECEIVED" (shown to the recipient so they can accept/decline)
 * Both are returned newest-first so the page reads naturally top-to-bottom.
 */
@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    @Query("SELECT r FROM FriendRequest r WHERE r.sender.username = :username ORDER BY r.createdAt DESC")
    List<FriendRequest> findBySenderUsername(@Param("username") String username);

    @Query("SELECT r FROM FriendRequest r WHERE r.recipient.username = :username ORDER BY r.createdAt DESC")
    List<FriendRequest> findByRecipientUsername(@Param("username") String username);

    /** Any pending request from a specific sender to a specific recipient. */
    Optional<FriendRequest> findBySenderUsernameAndRecipientUsernameAndStatus(
            String sender, String recipient, FriendRequest.Status status);

    /** All requests (sent or received) that this player is involved with, newest first. */
    @Query("SELECT r FROM FriendRequest r WHERE r.sender.username = :username OR r.recipient.username = :username ORDER BY r.createdAt DESC")
    List<FriendRequest> findByInvolvingUsername(@Param("username") String username);
}