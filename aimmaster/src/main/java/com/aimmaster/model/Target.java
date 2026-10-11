package com.aimmaster.model;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A single target flying around a room's arena (a bird, balloon, or fruit).
 * This is a plain in-memory object - it is never saved to the database, because
 * targets only exist for the lifetime of one live match (see AimMaster_01_Project_Overview.md
 * Section 7: "model" holds domain objects not backed by DB).
 *
 * THREAD SAFETY (Sprint 4 pitfall: "don't mutate the shared Target list from both the
 * game loop thread and an incoming WebSocket handler thread without synchronization"):
 *   - x/y/velocity are only ever written by ONE thread: this room's own GameSession
 *     loop thread (see GameEngine.move). No other thread touches them, so plain
 *     doubles are safe - there's nothing to race.
 *   - `alive` is different: the game loop thread can despawn a target (off-screen),
 *     AND (from Sprint 5 onward) a WebSocket message-handling thread will mark a
 *     target dead when a player's shot lands. Two different threads writing the same
 *     flag is exactly a race condition, so `alive` is an AtomicBoolean and dying is
 *     done through tryMarkDead(), which atomically flips true->false and reports
 *     whether THIS call was the one that won - Sprint 5's HitResolver depends on
 *     that return value to guarantee only one shot ever scores a point per target.
 */
public class Target {

    private final String id;
    private final TargetType type;
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private final AtomicBoolean alive = new AtomicBoolean(true);

    public Target(TargetType type, double x, double y, double velocityX, double velocityY) {
        this.id = UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    /**
     * Moves this target by its velocity, scaled by how much time passed (in seconds)
     * since the last tick. Keeping this on Target itself (instead of stuffing the
     * math into whatever calls it) is why Target stays a clean, single-responsibility class.
     * Only ever called from this room's single game-loop thread (see class javadoc).
     */
    public void move(double elapsedSeconds) {
        this.x += this.velocityX * elapsedSeconds;
        this.y += this.velocityY * elapsedSeconds;
    }

    /**
     * Marks this target as destroyed unconditionally. Used by the game loop for
     * housekeeping (e.g. a target drifted off-screen) where "did I win a race to kill
     * it" doesn't matter - only one thread (the game loop) ever calls this one.
     */
    public void markDead() {
        this.alive.set(false);
    }

    /**
     * Atomically tries to kill this target, and reports whether THIS call is the one
     * that actually did it. This is the primitive Sprint 5's HitResolver uses so that
     * two near-simultaneous shots on the same target can't both score: whichever
     * thread's tryMarkDead() returns true is the only one allowed to award a point.
     *
     * @return true if this call transitioned the target from alive to dead;
     *         false if it was already dead (someone else got there first, or it
     *         despawned naturally).
     */
    public boolean tryMarkDead() {
        return this.alive.compareAndSet(true, false);
    }

    public String getId() {
        return id;
    }

    public TargetType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public boolean isAlive() {
        return alive.get();
    }

    @Override
    public String toString() {
        return "Target{id='%s', type=%s, x=%.1f, y=%.1f, alive=%s}"
                .formatted(id, type, x, y, alive.get());
    }
}
