package com.aimmaster.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A permanent record of one player's result in one finished match (FR8). Written
 * exactly once, at match end - see service.MatchRecordService.persistMatchRecords(),
 * which is only ever called from GameSession's stop() path, never from the ~100ms
 * game loop tick (see AimMaster_03_Agent_Instructions.md Section 4: "persistence
 * only at defined boundaries").
 *
 * `accuracy` is stored as a plain fraction (0.0 - 1.0 = hits / total fire attempts,
 * or 0.0 if the player never fired at all) - DTOs are free to format that as a
 * percentage for display without the database needing to care about formatting.
 */
@Entity
@Table(name = "match_records")
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private double accuracy;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    protected MatchRecord() {
    }

    public MatchRecord(Player player, Room room, int score, double accuracy) {
        this.player = player;
        this.room = room;
        this.score = score;
        this.accuracy = accuracy;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public Room getRoom() {
        return room;
    }

    public int getScore() {
        return score;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
