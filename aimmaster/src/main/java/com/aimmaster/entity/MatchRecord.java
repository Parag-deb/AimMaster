package com.aimmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "match_records")
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Integer playerId;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Double accuracy;

    @Column(name = "shots_fired", nullable = false)
    private Integer shotsFired;

    @Column(name = "hits", nullable = false)
    private Integer hits;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected MatchRecord() {
    }

    public MatchRecord(Integer playerId, Integer roomId, Integer score,
                       Double accuracy, Integer shotsFired, Integer hits, Instant timestamp) {
        this.playerId = playerId;
        this.roomId = roomId;
        this.score = score;
        this.accuracy = accuracy;
        this.shotsFired = shotsFired;
        this.hits = hits;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public Integer getScore() {
        return score;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public Integer getShotsFired() {
        return shotsFired;
    }

    public Integer getHits() {
        return hits;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}