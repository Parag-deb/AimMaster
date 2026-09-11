package com.aimmaster.service;

import com.aimmaster.model.Position;
import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HitResolver thread-safety.
 *
 * Verifies that concurrent hits on the same target are handled correctly:
 * only one shooter should score, no double-counting.
 */
class HitResolverTest {

    private HitResolver hitResolver;
    private Target target;

    @BeforeEach
    void setUp() {
        hitResolver = new HitResolver();
        target = new Target(1, TargetType.BIRD, new Position(100, 100), 10, 0);
    }

    @Test
    void singleHit_returnsTrue() {
        boolean result = hitResolver.resolveHit(target, 1);
        assertTrue(result);
        assertFalse(target.isAlive());
    }

    @Test
    void secondHitOnSameTarget_returnsFalse() {
        hitResolver.resolveHit(target, 1);
        boolean result = hitResolver.resolveHit(target, 2);
        assertFalse(result);
    }

    @Test
    void hitOnNullTarget_returnsFalse() {
        boolean result = hitResolver.resolveHit(null, 1);
        assertFalse(result);
    }

    @Test
    void concurrentHitsOnSameTarget_onlyOneScores() throws InterruptedException {
        int numShooters = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numShooters);
        CountDownLatch latch = new CountDownLatch(numShooters);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numShooters; i++) {
            final int shooterId = i + 1;
            executor.submit(() -> {
                try {
                    if (hitResolver.resolveHit(target, shooterId)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly one shooter should succeed
        assertEquals(1, successCount.get(),
                "Only one concurrent hit should succeed, but got " + successCount.get());
        assertFalse(target.isAlive(), "Target should be dead after first hit");
    }
}