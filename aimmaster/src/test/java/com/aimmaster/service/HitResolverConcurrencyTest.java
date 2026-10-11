package com.aimmaster.service;

import com.aimmaster.model.RoomArena;
import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5's Definition of Done, as an automated test instead of a manual Postman
 * click-race: two "players" fire at the SAME target at (as close as the JVM allows
 * to) the SAME millisecond. Exactly one of them must be told they hit it, and the
 * leaderboard must show exactly one point awarded - never zero, never two.
 *
 * No Spring context needed here - HitResolver, RoomArena and Leaderboard are all
 * plain objects, so this runs as a fast, pure unit test.
 */
class HitResolverConcurrencyTest {

    @Test
    void twoSimultaneousShotsOnTheSameTargetOnlyScoreOnce() throws InterruptedException {
        RoomArena arena = new RoomArena();
        Target target = new Target(TargetType.FRUIT, 500, 300, 0, 0);
        arena.spawnTarget(target);

        HitResolver hitResolver = new HitResolver();
        Leaderboard leaderboard = new Leaderboard();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger hitCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<String> shooters = List.of("alice", "bob");

        for (String shooter : shooters) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                Target hit = hitResolver.resolveHit(arena, target.getId());
                if (hit != null) {
                    hitCount.incrementAndGet();
                    leaderboard.addPoint(shooter);
                }
            });
        }

        ready.await();
        go.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS), "worker threads should finish quickly");

        assertEquals(1, hitCount.get(), "exactly one of the two simultaneous shots should have hit");

        int totalPointsAwarded = leaderboard.standings().stream()
                .mapToInt(entry -> entry.getScore())
                .sum();
        assertEquals(1, totalPointsAwarded, "exactly one point total should have been awarded across both players");
    }
}
