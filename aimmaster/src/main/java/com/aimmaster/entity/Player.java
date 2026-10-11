package com.aimmaster.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A registered player account, now backed by the database.
 *
 * This is the Sprint 1 Player class adapted (not forked) into a real JPA entity:
 * the identity/username idea carries over, but "currentRoom" and "score" from the
 * Sprint 1 version don't belong here anymore - those are live, per-match values
 * that will be tracked in memory by GameSession (Sprint 4) and saved as a
 * MatchRecord at match end (Sprint 6). An account shouldn't "remember" a room
 * after the player logs back in a week later.
 *
 * "@Entity" tells Spring "this class maps to a database table" - Spring Data JPA
 * creates/reads/updates rows for us, we never write raw SQL for basic CRUD.
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    // Never store a raw password - only the hashed version (see AuthService).
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 20)
    private String theme;

    @Column(nullable = false)
    private boolean soundEnabled = true;

    @Column(nullable = false)
    private boolean musicEnabled = true;

    @Column(nullable = false)
    private boolean showHud = true;

    // JPA requires a no-argument constructor so it can build empty objects and fill them in.
    protected Player() {
    }

    public Player(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
    }

    public boolean isShowHud() {
        return showHud;
    }

    public void setShowHud(boolean showHud) {
        this.showHud = showHud;
    }
}
