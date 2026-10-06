package com.aimmaster.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

class FireRateLimiterTest {

    private FireRateLimiter fireRateLimiter;

    @BeforeEach
    void setUp() {
        fireRateLimiter = new FireRateLimiter(300, () -> 1000L);
    }

    @Test
    void firstFire_isAllowed() {
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
    }

    @Test
    void immediateSecondFire_isRejected() {
        fireRateLimiter.tryAcquire(1, 1);
        assertFalse(fireRateLimiter.tryAcquire(1, 1));
    }

    @Test
    void fireAfterCooldown_isAllowed() {
        fireRateLimiter = new FireRateLimiter(300, () -> 1000L);
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
        assertFalse(fireRateLimiter.tryAcquire(1, 1));
        // Advance time past cooldown
        fireRateLimiter = new FireRateLimiter(300, () -> 1301L);
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
    }

    @Test
    void differentPlayers_areIndependent() {
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
        assertTrue(fireRateLimiter.tryAcquire(1, 2));
        assertFalse(fireRateLimiter.tryAcquire(1, 1));
        assertFalse(fireRateLimiter.tryAcquire(1, 2));
    }

    @Test
    void differentRooms_areIndependent() {
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
        assertTrue(fireRateLimiter.tryAcquire(2, 1));
        assertFalse(fireRateLimiter.tryAcquire(1, 1));
        assertFalse(fireRateLimiter.tryAcquire(2, 1));
    }

    @Test
    void concurrentFires_onlyOneAllowedPerWindow() throws InterruptedException {
        fireRateLimiter = new FireRateLimiter(300, () -> 1000L);
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger allowedCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    if (fireRateLimiter.tryAcquire(1, 1)) {
                        allowedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, allowedCount.get(),
                "Only one concurrent fire should be allowed within the cooldown window");
    }

    @Test
    void clearRoom_removesState() {
        fireRateLimiter.tryAcquire(1, 1);
        assertFalse(fireRateLimiter.tryAcquire(1, 1));
        fireRateLimiter.clearRoom(1);
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
    }

    @Test
    void clearAll_removesAllState() {
        fireRateLimiter.tryAcquire(1, 1);
        fireRateLimiter.tryAcquire(2, 1);
        assertFalse(fireRateLimiter.tryAcquire(1, 1));
        assertFalse(fireRateLimiter.tryAcquire(2, 1));
        fireRateLimiter.clearAll();
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
        assertTrue(fireRateLimiter.tryAcquire(2, 1));
    }

    @Test
    void negativeInterval_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new FireRateLimiter(-1, System::currentTimeMillis));
    }

    @Test
    void zeroInterval_allowsEveryFire() {
        fireRateLimiter = new FireRateLimiter(0, () -> 1000L);
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
        assertTrue(fireRateLimiter.tryAcquire(1, 1));
    }
}
