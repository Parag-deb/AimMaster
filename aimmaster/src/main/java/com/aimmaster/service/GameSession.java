package com.aimmaster.service;

import com.aimmaster.dto.ArenaStateMessage;
import com.aimmaster.dto.TargetSnapshot;
import com.aimmaster.entity.Room;
import com.aimmaster.model.Difficulty;
import com.aimmaster.model.RoomArena;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Owns the ENTIRE lifecycle of one room's live match: its own background thread,
 * its own arena, its own start/stop state. This is the concurrency backbone the
 * project is graded on - see AimMaster_01_Project_Overview.md Section 1.
 *
 * ISOLATION: every GameSession gets its OWN ScheduledExecutorService (one dedicated
 * thread), created fresh in the constructor below. Two rooms never share a thread or
 * an executor, so one room being slow/stuck can never delay another room's ticks -
 * that's what the Sprint 4 Definition of Done actually tests.
 *
 * GameSession itself does NOT decide what happens each tick (that's GameEngine's job),
 * track which rooms are currently running (that's GameSessionManager's job), or save
 * anything to the database (that's Sprint 6's MatchRecordService's job, triggered via
 * the onEnded callback below) - it only owns the thread, the schedule, and this one
 * match's live in-memory numbers. Keeping these responsibilities in separate classes
 * is what keeps each one small and easy to reason about.
 */
public class GameSession {

    private static final Logger log = LoggerFactory.getLogger(GameSession.class);
    private static final long TICK_INTERVAL_MS = 100; // ~10 updates/second, per the NFR on perceived latency

    private final Room room;
    private final RoomArena arena = new RoomArena();
    private final Leaderboard leaderboard = new Leaderboard(); // Sprint 5: one live scoreboard per match

    // Sprint 6: who was in the room when the match started, and how many times each
    // of them fired (hit or miss) - both needed at match end to compute accuracy and
    // to know who to write a MatchRecord for, even a player who scored zero points.
    private final Set<String> participantUsernames;
    private final Map<String, AtomicInteger> fireAttempts = new ConcurrentHashMap<>();

    private final GameEngine gameEngine;
    private final RoomBroadcaster roomBroadcaster;
    private final Consumer<GameSession> onEnded; // notifies GameSessionManager once, however the match ended

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private volatile Instant startedAt;
    private volatile ScheduledFuture<?> tickFuture;

    public GameSession(Room room, Set<String> participantUsernames, GameEngine gameEngine,
                        RoomBroadcaster roomBroadcaster, Consumer<GameSession> onEnded) {
        this.room = room;
        this.participantUsernames = Set.copyOf(participantUsernames); // defensive, immutable snapshot
        this.gameEngine = gameEngine;
        this.roomBroadcaster = roomBroadcaster;
        this.onEnded = onEnded;
    }

    /** Starts the game loop. Throws if this session was already started once (sessions aren't reusable). */
    public void start() {
        if (!active.compareAndSet(false, true)) {
            throw new IllegalStateException("GameSession for room " + room.getCode() + " is already running");
        }

        startedAt = Instant.now();

        // Pre-populate the leaderboard with every participant at score 0 so the
        // host and players see all names from the very first frame - no empty
        // scoreboard until someone lands their first hit.
        for (String username : participantUsernames) {
            leaderboard.initializeScore(username, 0);
        }

        // scheduleAtFixedRate keeps ticking every TICK_INTERVAL_MS regardless of how long
        // each individual tick took, so the tick rate stays steady even under minor load.
        tickFuture = executor.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Auto-end the match once its configured duration has passed (FR10: "or it
        // auto-ends at configured duration"). This runs once, on the same executor.
        executor.schedule(this::stop, room.getDurationSeconds(), TimeUnit.SECONDS);

        log.info("GameSession started for room {} (difficulty={}, duration={}s, players={})",
                room.getCode(), room.getDifficulty(), room.getDurationSeconds(), participantUsernames);
    }

    /**
     * Stops the loop and releases its thread. Safe to call more than once - only the
     * FIRST call actually does anything (guarded by the active flag's compareAndSet),
     * which matters because stop() can be reached two different ways: a host manually
     * ending the match, OR the duration timer firing on its own. Either way, onEnded
     * fires exactly once here, in one place, so Sprint 6's persistence step can never
     * accidentally run twice for the same match.
     */
    public void stop() {
        if (active.compareAndSet(true, false)) {
            if (tickFuture != null) {
                tickFuture.cancel(false); // let an in-progress tick finish; just don't schedule another
            }
            executor.shutdown(); // no thread leak - this room's dedicated thread is released
            log.info("GameSession stopped for room {}", room.getCode());
            onEnded.accept(this);
        }
    }

    private void tick() {
        try {
            double elapsedSeconds = Duration.between(startedAt, Instant.now()).toMillis() / 1000.0;
            gameEngine.tick(arena, elapsedSeconds, TICK_INTERVAL_MS / 1000.0, room.getDifficulty());
            broadcastArenaState(elapsedSeconds);
        } catch (Exception e) {
            // IMPORTANT beginner note: scheduleAtFixedRate silently stops scheduling ALL
            // future ticks forever if a task throws an uncaught exception - with no error,
            // no crash, nothing - the room just quietly freezes. Catching everything here
            // and logging it keeps a bug in one tick from silently killing the whole match.
            log.error("Unhandled error in game loop tick for room {}", room.getCode(), e);
        }
    }

    private void broadcastArenaState(double elapsedSeconds) {
        List<TargetSnapshot> snapshots = arena.getTargets().stream()
                .map(TargetSnapshot::from)
                .toList();
        ArenaStateMessage message = new ArenaStateMessage(room.getCode(), elapsedSeconds, snapshots);
        roomBroadcaster.broadcastToRoom(room.getCode(), message);
    }

    /** Called by RoomWebSocketController for every fire event (hit AND miss) - accuracy needs both counts. */
    public void recordFireAttempt(String username) {
        fireAttempts.computeIfAbsent(username, u -> new AtomicInteger()).incrementAndGet();
    }

    public int getFireAttempts(String username) {
        AtomicInteger count = fireAttempts.get(username);
        return count == null ? 0 : count.get();
    }

    public boolean isActive() {
        return active.get();
    }

    /**
     * Adds a player to the live leaderboard with score 0 so they appear
     * immediately when they join mid-match, before their first hit.
     */
    public void addPlayerToLeaderboard(String username) {
        leaderboard.initializeScore(username, 0);
    }

    /**
     * Sprint 9 (reconnect handling): how far into the match we are right now,
     * computed fresh on demand rather than cached - safe to call from any thread,
     * any time, including from an HTTP request thread handling GET
     * /api/rooms/{code}/live, completely independent of this room's own tick
     * thread. Returns 0 if the match hasn't started yet (shouldn't normally happen,
     * since a GameSession isn't created until start() is about to run, but this
     * keeps the method safe rather than throwing on a technicality).
     */
    public double getElapsedSeconds() {
        Instant start = startedAt;
        return start == null ? 0.0 : Duration.between(start, Instant.now()).toMillis() / 1000.0;
    }

    public RoomArena getArena() {
        return arena;
    }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public String getRoomCode() {
        return room.getCode();
    }

    /** Sprint 9 (admin dashboard): this match's configured difficulty, straight off the Room entity. */
    public Difficulty getDifficulty() {
        return room.getDifficulty();
    }

    /** Everyone who was a member of the room at the moment this match started. */
    public Set<String> getParticipantUsernames() {
        return Collections.unmodifiableSet(participantUsernames);
    }

    /**
     * Sprint 8: everyone who was in the room at match start, UNIONED with anyone
     * who joined mid-match and actually fired or scored. Without this, a player who
     * joined after the host clicked "start" could rack up a real score on the live
     * leaderboard - visible to everyone in the room in real time - and then simply
     * vanish from history the moment the match ended, because
     * getParticipantUsernames() only knows about who was there at the start. This
     * is what MatchRecordService actually persists from (see Sprint 8 notes).
     */
    public Set<String> getAllInvolvedUsernames() {
        Set<String> all = new HashSet<>(participantUsernames);
        all.addAll(fireAttempts.keySet());
        all.addAll(leaderboard.getUsernames());
        return all;
    }
}
