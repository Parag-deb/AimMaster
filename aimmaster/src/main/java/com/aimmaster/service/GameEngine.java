package com.aimmaster.service;

import com.aimmaster.model.Difficulty;
import com.aimmaster.model.RoomArena;
import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The actual per-tick game logic (FR15): move every live target, despawn anything
 * that's drifted off-screen, and spawn new targets at a rate that gets faster the
 * longer the match runs. This class is intentionally separate from GameSession -
 * GameSession's job is owning the THREAD that calls tick() on a schedule; this
 * class's job is deciding WHAT happens on each tick. Splitting them keeps each class
 * doing exactly one thing (no god-class).
 *
 * GameEngine itself holds no per-room mutable state (every room's arena/settings are
 * passed in as parameters), so one shared instance is safely reused across every
 * room's GameSession at once - there's nothing here for two rooms to race over.
 */
@Component
public class GameEngine {

    // Simple fixed arena size in arbitrary units. Sprint 7's Canvas frontend will use
    // these same dimensions (scaled to the actual screen) so target positions line up.
    public static final double ARENA_WIDTH = 1000.0;
    public static final double ARENA_HEIGHT = 600.0;
    private static final double OFFSCREEN_MARGIN = 60.0;

    // How much harder the match gets per second elapsed, on top of the difficulty's
    // starting point. Deliberately simple, tunable numbers - see Overview Section 4:
    // "complex physics engine" is explicitly out of scope, plain drift is enough.
    private static final double SPAWN_INTERVAL_SHRINK_PER_SECOND_MS = 12.0;
    private static final double MIN_SPAWN_INTERVAL_MS = 350.0;
    private static final double SPEED_GROWTH_PER_SECOND = 0.015; // +1.5% speed per second elapsed

    /**
     * Advances one room's arena by one tick. Called by that room's GameSession every
     * ~100ms, always from that same single game-loop thread - see RoomArena/Target's
     * thread-safety notes for why that single-writer guarantee matters.
     *
     * @param arena           the room's live target state
     * @param elapsedSeconds  how long the match has been running, in seconds
     * @param deltaSeconds    how much time this one tick covers (normally ~0.1s)
     * @param difficulty      the room's configured difficulty
     */
    public void tick(RoomArena arena, double elapsedSeconds, double deltaSeconds, Difficulty difficulty) {
        moveAndDespawn(arena, deltaSeconds);
        maybeSpawn(arena, elapsedSeconds, difficulty);
    }

    private void moveAndDespawn(RoomArena arena, double deltaSeconds) {
        for (Target target : arena.getTargets()) {
            if (!target.isAlive()) {
                continue;
            }
            target.move(deltaSeconds);
            if (isOffScreen(target)) {
                target.markDead(); // housekeeping despawn, not a hit - see Target's javadoc
            }
        }
        arena.removeDeadTargets();
    }

    private boolean isOffScreen(Target target) {
        return target.getX() < -OFFSCREEN_MARGIN
                || target.getX() > ARENA_WIDTH + OFFSCREEN_MARGIN
                || target.getY() < -OFFSCREEN_MARGIN
                || target.getY() > ARENA_HEIGHT + OFFSCREEN_MARGIN;
    }

    private void maybeSpawn(RoomArena arena, double elapsedSeconds, Difficulty difficulty) {
        double spawnIntervalSeconds = currentSpawnIntervalMs(difficulty, elapsedSeconds) / 1000.0;

        if (elapsedSeconds - arena.getLastSpawnElapsedSeconds() >= spawnIntervalSeconds) {
            arena.spawnTarget(newRandomTarget(difficulty, elapsedSeconds));
            arena.setLastSpawnElapsedSeconds(elapsedSeconds);
        }
    }

    private double currentSpawnIntervalMs(Difficulty difficulty, double elapsedSeconds) {
        double shrunk = difficulty.getBaseSpawnIntervalMs() - (elapsedSeconds * SPAWN_INTERVAL_SHRINK_PER_SECOND_MS);
        return Math.max(MIN_SPAWN_INTERVAL_MS, shrunk);
    }

    private double currentSpeed(Difficulty difficulty, double elapsedSeconds) {
        return difficulty.getBaseSpeed() * (1 + elapsedSeconds * SPEED_GROWTH_PER_SECOND);
    }

    private Target newRandomTarget(Difficulty difficulty, double elapsedSeconds) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        TargetType[] types = TargetType.values();
        TargetType type = types[random.nextInt(types.length)];

        // Spawn from the right edge, drifting left - simple, predictable motion
        // that's easy for a beginner to read and easy for Sprint 7's frontend to render.
        double startY = random.nextDouble(50, ARENA_HEIGHT - 50);
        double speed = currentSpeed(difficulty, elapsedSeconds);
        double verticalDrift = random.nextDouble(-15, 15);

        return new Target(type, ARENA_WIDTH, startY, -speed, verticalDrift);
    }
}
