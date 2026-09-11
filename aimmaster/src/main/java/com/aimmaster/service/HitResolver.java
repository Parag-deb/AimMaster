package com.aimmaster.service;

import com.aimmaster.model.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe hit resolution service.
 *
 * Responsibility:
 * - Given a target and a player, atomically check if the target is still alive
 *   and mark it dead in a single synchronized operation.
 * - This prevents the race condition where two players shoot the same target
 *   at nearly the same time and both would score.
 *
 * The check-and-mark must be atomic — we do NOT use separate isAlive() check
 * followed by hit() because there's a gap where another thread could sneak in.
 * Instead, we synchronize on the target object itself during the entire operation.
 */
@Service
public class HitResolver {

    private static final Logger log = LoggerFactory.getLogger(HitResolver.class);

    /**
     * Attempts to register a hit on the given target by the given player.
     *
     * @param target   the target being shot at
     * @param playerId the ID of the player shooting
     * @return true if the hit was registered (target was alive), false if target was already dead
     */
    public boolean resolveHit(Target target, int playerId) {
        if (target == null) {
            return false;
        }

        // Synchronize on the target itself to make check-and-mark atomic.
        // This ensures only one thread can execute this block for a given target at a time.
        synchronized (target) {
            if (target.isAlive()) {
                target.hit();
                log.debug("Player {} hit target {}", playerId, target.getId());
                return true;
            } else {
                log.debug("Player {} missed target {} (already dead)", playerId, target.getId());
                return false;
            }
        }
    }
}