package com.aimmaster.dto;

/** Response for GET /api/players/me — the current user's account info. */
public class PlayerInfoResponse {

    private String username;
    private String theme;
    private boolean soundEnabled;
    private boolean musicEnabled;
    private boolean showHud;
    private String createdAt;

    public PlayerInfoResponse(String username, String theme, boolean soundEnabled, boolean musicEnabled, boolean showHud, String createdAt) {
        this.username = username;
        this.theme = theme;
        this.soundEnabled = soundEnabled;
        this.musicEnabled = musicEnabled;
        this.showHud = showHud;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public String getTheme() {
        return theme;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public boolean isShowHud() {
        return showHud;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
