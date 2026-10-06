package com.aimmaster.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * A mutual friendship between two registered players, persisted in the database.
 *
 * DESIGN DECISIONS:
 *   - Stored as ONE row per friendship (not two), with player1 always having the
 *     numerically smaller id. This means "are A and B friends?" is answered by a
 *     single lookup regardless of which player is asking - no need to check both
 *     (A,B) and (B,A) directions, and no risk of accidentally creating the same
 *     friendship twice from two concurrent requests.
 *   - The friendship is only ever CREATED from a FriendRequest flow (see
 *     FriendRequestService.accept()), never directly - that keeps "who sent the
 *     request" logic in exactly one place and prevents a stray direct save from
 *     producing an orphan friendship with no request behind it.
 *   - Equality is based on the pair of player ids, so a friendship between the same
 *     two people can never be persisted twice even under concurrency (the unique
 *     constraint backs this up at the DB level too).
 */
@Entity
@Table(name = "friends",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player1_id", "player2_id"}))
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player2_id", nullable = false)
    private Player player2;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // JPA requires a no-argument constructor so it can build empty objects and fill them in.
    protected Friend() {
    }

    public Friend(Player player1, Player player2) {
        // Caller is responsible for ordering: pass the smaller-id player first.
        // FriendRequestService.accept() does this ordering for them.
        this.player1 = player1;
        this.player2 = player2;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** True if the given player is one of the two friends in this row. */
    public boolean involves(String username) {
        return player1.getUsername().equals(username) || player2.getUsername().equals(username);
    }

    /** The OTHER player in this friendship, or null if username isn't part of this row. */
    public String otherUsername(String username) {
        if (player1.getUsername().equals(username)) {
            return player2.getUsername();
        }
        if (player2.getUsername().equals(username)) {
            return player1.getUsername();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Friend other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}