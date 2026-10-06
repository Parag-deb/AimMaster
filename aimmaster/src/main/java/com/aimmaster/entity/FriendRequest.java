package com.aimmaster.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A request from one player to another, asking to become friends. Persists until
 * either the recipient ACCEPTS (which creates a Friend row and deletes this one),
 * the recipient DECLINES (which just deletes this one), the sender CANCELS, or
 * the sender withdraws their account.
 *
 * WHY A SEPARATE ENTITY (not just a "friends" table with a status column):
 *   - A friendship is MUTUAL and permanent; a pending request is one-directional
 *     (sender -> recipient) and temporary. Modeling them as two different things
 *     keeps each concept's queries simple: "who are my friends?" is one join,
 *     "who has asked me to be friends?" is one lookup, and neither query has to
 *     filter by a status flag.
 *   - The unique constraint below prevents the classic "send the same request
 *     twice" bug: two rows with the same (sender, recipient) can't coexist, so a
 *     player can't accidentally send ten identical requests that all need to be
 *     individually declined.
 *
 * Statuses are deliberately a plain enum rather than a string column: the valid
 * set is tiny and fixed, and an enum gives compile-time safety over a free-form
 * string that could drift into typos over time.
 */
@Entity
@Table(name = "friend_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "recipient_id"}))
public class FriendRequest {

    public enum Status { PENDING, ACCEPTED, DECLINED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Player sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Player recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA requires a no-argument constructor so it can build empty objects and fill them in.
    protected FriendRequest() {
    }

    public FriendRequest(Player sender, Player recipient) {
        this.sender = sender;
        this.recipient = recipient;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Player getSender() {
        return sender;
    }

    public Player getRecipient() {
        return recipient;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Called whenever the request's status changes - keeps updatedAt honest. */
    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /** True if the given player is the sender or the recipient of this request. */
    public boolean involves(String username) {
        return sender.getUsername().equals(username) || recipient.getUsername().equals(username);
    }
}