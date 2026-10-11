package com.aimmaster.model;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live, in-memory arena state for ONE currently-running match: just the set of
 * targets currently flying around. This is deliberately separate from entity.Room
 * (see that class's "hybrid persistence" javadoc from Sprint 2) - a Room row in the
 * database describes a room's settings forever, but a RoomArena only exists while a
 * match is actually running and is thrown away when it ends.
 *
 * THREAD SAFETY: a ConcurrentHashMap is used (not a plain HashMap/ArrayList) because,
 * from Sprint 5 onward, this map is read/written by two different kinds of threads at
 * once: this room's own game-loop thread (moving/spawning/despawning every ~100ms)
 * and whichever thread handles an incoming "/app/room/{roomId}/fire" STOMP message
 * (looking a target up by id to check it). ConcurrentHashMap allows safe concurrent
 * reads/writes without us hand-writing synchronized blocks around every access - see
 * AimMaster_03_Agent_Instructions.md's locked concurrency toolset.
 */
public class RoomArena {

    private final ConcurrentHashMap<String, Target> targets = new ConcurrentHashMap<>();

    // How far (in elapsed match-seconds) into the match we were when we last spawned a
    // target. Only ever read/written by this room's single game-loop thread (see
    // GameEngine), so a plain double is safe here - same reasoning as Target's x/y.
    private double lastSpawnElapsedSeconds = 0.0;

    public void spawnTarget(Target target) {
        targets.put(target.getId(), target);
    }

    /** Removes every target that's been marked dead (despawned or hit), so the map doesn't grow forever. */
    public void removeDeadTargets() {
        targets.values().removeIf(t -> !t.isAlive());
    }

    /** Look up a target by id - this is what Sprint 5's HitResolver will use to find the target a shot named. */
    public Target findById(String targetId) {
        return targets.get(targetId);
    }

    /**
     * A safe-to-iterate live view of the current targets. Backed directly by the
     * ConcurrentHashMap, so reading this while the game loop is mid-update on another
     * call is safe (won't throw ConcurrentModificationException) - it just reflects
     * whatever state existed at the moment each element was read.
     */
    public Collection<Target> getTargets() {
        return Collections.unmodifiableCollection(targets.values());
    }

    public double getLastSpawnElapsedSeconds() {
        return lastSpawnElapsedSeconds;
    }

    public void setLastSpawnElapsedSeconds(double lastSpawnElapsedSeconds) {
        this.lastSpawnElapsedSeconds = lastSpawnElapsedSeconds;
    }

    public int size() {
        return targets.size();
    }
}
