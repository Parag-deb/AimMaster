package com.aimmaster.service;

import com.aimmaster.dto.LeaderboardEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live, in-memory score tracking for ONE currently-running match (FR7). Just like
 * RoomArena, this is deliberately NOT a database table - see
 * AimMaster_03_Agent_Instructions.md Section 4: "don't recompute the whole leaderboard
 * by reloading from the DB on every hit... keep it in-memory per room for the live
 * loop, persist only at Sprint 6's defined boundary" (match end, as a MatchRecord).
 *
 * One Leaderboard belongs to exactly one GameSession/match, created fresh when a
 * match starts and discarded when it ends - same lifecycle as RoomArena.
 *
 * THREAD SAFETY: addPoint() can be called concurrently by multiple players' fire
 * events landing at nearly the same time (different STOMP messages, potentially
 * handled on different threads), so scores live in a ConcurrentHashMap of
 * AtomicIntegers - incrementAndGet() is a single atomic operation, so two hits by the
 * same player at the same instant can never silently overwrite each other's point.
 */
public class Leaderboard {

    private final Map<String, AtomicInteger> scoresByUsername = new ConcurrentHashMap<>();

    /** Awards one point to this username and returns their new total score. */
    public int addPoint(String username) {
        return scoresByUsername
                .computeIfAbsent(username, u -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /** Sets a player's score to exactly 0, keeping their entry in the map so they
     *  still appear in standings() with a zero score. */
    public void resetScore(String username) {
        scoresByUsername.computeIfAbsent(username, u -> new AtomicInteger(0)).set(0);
    }

    /**
     * Initializes a player's score to a specific value (typically 0) without
     * incrementing. Useful for pre-populating the leaderboard at match start or
     * adding mid-match joiners. If the player already has a score, it is
     * overwritten to the given value.
     */
    public void initializeScore(String username, int score) {
        scoresByUsername.computeIfAbsent(username, u -> new AtomicInteger(score)).set(score);
    }

    public int getScore(String username) {
        AtomicInteger score = scoresByUsername.get(username);
        return score == null ? 0 : score.get();
    }

    /**
     * Everyone who has scored at least one point so far. Used by Sprint 8's
     * getAllInvolvedUsernames() so a player who joined mid-match and actually
     * scored isn't silently left out of match history at persistence time.
     */
    public Set<String> getUsernames() {
        return Set.copyOf(scoresByUsername.keySet());
    }

    /** Current standings, highest score first. Used by Sprint 6's non-real-time snapshot endpoint. */
    public List<LeaderboardEntry> standings() {
        return scoresByUsername.entrySet().stream()
                .map(e -> new LeaderboardEntry(e.getKey(), e.getValue().get()))
                .sorted(Comparator.comparingInt(LeaderboardEntry::getScore).reversed())
                .toList();
    }
}
