package com.aimmaster.dto;

import java.util.List;

/**
 * Sprint 9 (admin dashboard, FR11): a whole-server snapshot. jvmActiveThreadCount
 * comes straight from Thread.activeCount() - a deliberately simple, honest number
 * (not a fancy custom thread registry) that visibly grows by roughly one every time
 * a room's match starts and shrinks by one every time it stops, since each
 * GameSession owns exactly one dedicated thread (see GameSession's javadoc). Watching
 * this number during a demo is a direct, live illustration of the project's
 * per-room-thread concurrency design - not just a claim in a report.
 */
public class AdminSystemStatsResponse {

    private final int activeMatchCount;
    private final int jvmActiveThreadCount;
    private final List<AdminRoomStatsResponse> rooms;

    public AdminSystemStatsResponse(int activeMatchCount, int jvmActiveThreadCount,
                                     List<AdminRoomStatsResponse> rooms) {
        this.activeMatchCount = activeMatchCount;
        this.jvmActiveThreadCount = jvmActiveThreadCount;
        this.rooms = rooms;
    }

    public int getActiveMatchCount() {
        return activeMatchCount;
    }

    public int getJvmActiveThreadCount() {
        return jvmActiveThreadCount;
    }

    public List<AdminRoomStatsResponse> getRooms() {
        return rooms;
    }
}
