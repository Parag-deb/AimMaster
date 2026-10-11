package com.aimmaster.service;

import com.aimmaster.model.RoomArena;
import com.aimmaster.model.Target;
import org.springframework.stereotype.Component;

/**
 * FR13/FR14: given a fire event's target id, decide hit or miss - and guarantee that
 * if two players' shots name the same target at nearly the same millisecond, only
 * ONE of them can ever be told "you hit it."
 *
 * This class does almost nothing on its own on purpose: the actual race-safety comes
 * from Target.tryMarkDead()'s AtomicBoolean.compareAndSet, built back in Sprint 4
 * specifically so this moment wouldn't need its own locking. HitResolver's whole job
 * is just "look the target up, ask it to atomically die, report what happened" - a
 * classic naive bug here would be:
 *
 *     if (target.isAlive()) {       // Thread A and Thread B BOTH see "alive" here
 *         target.markDead();        // BOTH threads then mark it dead
 *         award point to A AND B    // double-scored - exactly what we must avoid
 *     }
 *
 * tryMarkDead() collapses that "check, then separately act" gap into one atomic step,
 * so there's no window where two threads can both see "still alive."
 *
 * Like GameEngine, HitResolver holds no per-room state itself (the RoomArena is
 * passed in), so one shared instance is safe to reuse across every room at once.
 */
@Component
public class HitResolver {

    /**
     * @return the Target that was hit, if this call is the one that legitimately
     *         killed it; null if there's no such target, or it was already dead
     *         (already hit by someone else, or despawned) - either way, a miss.
     */
    public Target resolveHit(RoomArena arena, String targetId) {
        if (targetId == null) {
            return null;
        }
        Target target = arena.findById(targetId);
        if (target == null) {
            return null; // unknown/already-removed target id - treat as a miss, not an error
        }
        return target.tryMarkDead() ? target : null;
    }
}
