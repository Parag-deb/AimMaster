package com.aimmaster.entity;

import com.aimmaster.model.Difficulty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A match room, now backed by the database.
 *
 * DESIGN DECISION (Sprint 2 task: "decide and document" persisted vs. in-memory Room):
 * Room is a HYBRID.
 *   - PERSISTED here: room code, host, settings (difficulty/duration/maxPlayers), and
 *     which players have joined. This is small, slow-changing data - fine for the DB.
 *   - NOT persisted / stays in-memory only: live target positions, timers, and the
 *     running game loop. That data changes every ~100ms (Sprint 4's GameSession) and
 *     writing it to a database on every tick would both be slow and pointless once the
 *     match ends. Sprint 1's Target class therefore stays a plain in-memory object -
 *     it never gets an @Entity annotation.
 * This matches AimMaster_03_Agent_Instructions.md Section 4: "persistence only at
 * defined boundaries" - not inside the hot game loop.
 *
 * The Difficulty enum is reused as-is from com.aimmaster.model (Sprint 1) - no need
 * to fork a second copy just because this class is now an entity.
 */
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private Player host;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Who has joined this room so far. This is persisted membership (useful for
    // "who was in this room" history / reconnects later) - it is NOT the live
    // in-memory game state.
    @ManyToMany
    @JoinTable(
            name = "room_players",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private Set<Player> players = new LinkedHashSet<>();

    protected Room() {
    }

    public Room(String code, Player host, Difficulty difficulty, int durationSeconds, int maxPlayers) {
        this.code = code;
        this.host = host;
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
        this.maxPlayers = maxPlayers;
        this.createdAt = Instant.now();
        this.players.add(host); // host automatically counts as a joined player
    }

    /** @return true if adding one more player would exceed maxPlayers */
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void addPlayer(Player player) {
        if (isFull()) {
            throw new IllegalStateException("Room " + code + " is full");
        }
        players.add(player);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Player getHost() {
        return host;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
