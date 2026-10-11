package com.aimmaster.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves Target.tryMarkDead() cannot be "won" by two threads at once - this is the
 * exact race condition Sprint 4's pitfall warns about, and the one Sprint 5's
 * HitResolver relies on being impossible. We fire two threads at the SAME target at
 * the SAME time (a CountDownLatch holds them both at the starting line so they hit
 * tryMarkDead() as close to simultaneously as the JVM allows) and assert exactly one
 * of them got `true` back.
 */
class TargetConcurrencyTest {

    @Test
    void onlyOneOfTwoSimultaneousShotsCanKillTheSameTarget() throws InterruptedException {
        Target target = new Target(TargetType.BALLOON, 0, 0, 0, 0);

        CountDownLatch readyLatch = new CountDownLatch(2); // both threads signal "I'm ready"
        CountDownLatch startLatch = new CountDownLatch(1); // main thread fires the starting gun
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable shoot = () -> {
            readyLatch.countDown();
            try {
                startLatch.await(); // both threads wait here until released together
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (target.tryMarkDead()) {
                successCount.incrementAndGet();
            }
        };

        executor.submit(shoot);
        executor.submit(shoot);

        readyLatch.await();          // wait for both threads to be lined up
        startLatch.countDown();      // release them at (as close as possible to) the same instant
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS), "threads should finish quickly");

        assertEquals(1, successCount.get(), "exactly one of the two simultaneous shots should have scored");
        assertFalse(target.isAlive());
    }
}
