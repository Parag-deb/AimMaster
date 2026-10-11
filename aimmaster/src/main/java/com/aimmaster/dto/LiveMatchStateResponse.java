package com.aimmaster.dto;

import java.util.List;

/**
 * Sprint 9 (reconnect handling): "catch me up" snapshot of a room's CURRENTLY
 * running match, fetched over plain REST right after a client (re)connects, so it
 * can repaint targets/leaderboard/elapsed time immediately instead of starting from
 * a blank screen and waiting for the next ~100ms broadcast tick (or, for the
 * leaderboard, waiting for someone to score again). This is exactly the locked
 * REST-vs-WebSocket split from Overview.md Section 7 in action: REST handles
 * "catch me up, this can wait a beat," WebSocket keeps things updated live
 * afterward.
 *
 * running=false with empty lists simply means no match is currently active in that
 * room - not an error.
 */
public class LiveMatchStateResponse {

    private final boolean running;
    private final double elapsedSeconds;
    private final List<TargetSnapshot> targets;
    private final List<LeaderboardEntry> leaderboard;

    public LiveMatchStateResponse(boolean running, double elapsedSeconds,
                                   List<TargetSnapshot> targets, List<LeaderboardEntry> leaderboard) {
        this.running = running;
        this.elapsedSeconds = elapsedSeconds;
        this.targets = targets;
        this.leaderboard = leaderboard;
    }

    public boolean isRunning() {
        return running;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public List<TargetSnapshot> getTargets() {
        return targets;
    }

    public List<LeaderboardEntry> getLeaderboard() {
        return leaderboard;
    }
}
