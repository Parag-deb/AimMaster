package com.aimmaster.repository;

import com.aimmaster.entity.Friend;
import com.aimmaster.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Handles the friends table. Every query is written in terms of a username
 * (not the numeric Player.id) because that's what the rest of the project uses
 * over the wire - see entity.Player / DTO naming notes elsewhere.
 */
@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    /**
     * Every friendship this username is part of, newest first. A friendship row
     * involves exactly two players, so we ask "where player1 is me OR player2 is
     * me" - the single-row-per-friendship design (see Friend's javadoc) means
     * this returns each friend exactly once, never a duplicate.
     */
    @Query("SELECT f FROM Friend f WHERE f.player1.username = :username OR f.player2.username = :username")
    List<Friend> findByInvolvingUsername(@Param("username") String username);

    /** True if these two players are already friends (in either direction). */
    boolean existsByPlayer1UsernameAndPlayer2Username(String p1, String p2);

    /** Find the exact friendship row where player1 is the smaller-id username. */
    Optional<Friend> findByPlayer1UsernameAndPlayer2Username(String p1, String p2);

    /** Convenience: same check but with the pair in either order - the caller
     *  doesn't have to remember which id is "player1" (the smaller one). */
    default boolean areFriends(String a, String b) {
        return existsByPlayer1UsernameAndPlayer2Username(a, b)
                || existsByPlayer1UsernameAndPlayer2Username(b, a);
    }
}