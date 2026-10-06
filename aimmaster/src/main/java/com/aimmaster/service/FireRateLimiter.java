package com.aimmaster.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

@Service
public class FireRateLimiter {

    private final long minimumIntervalMs;
    private final LongSupplier currentTimeMillis;
    private final Map<Integer, Map<Integer, Long>> lastFireAt = new ConcurrentHashMap<>();

    public FireRateLimiter(@Value("${app.game.fire-cooldown-ms:300}") long minimumIntervalMs) {
        this(minimumIntervalMs, System::currentTimeMillis);
    }

    FireRateLimiter(long minimumIntervalMs, LongSupplier currentTimeMillis) {
        if (minimumIntervalMs < 0) {
            throw new IllegalArgumentException("minimumIntervalMs must not be negative");
        }
        this.minimumIntervalMs = minimumIntervalMs;
        this.currentTimeMillis = currentTimeMillis;
    }

    public boolean tryAcquire(int roomId, int playerId) {
        long now = currentTimeMillis.getAsLong();
        Map<Integer, Long> playerTimestamps = lastFireAt.computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>());
        AtomicBoolean allowed = new AtomicBoolean(false);

        playerTimestamps.compute(playerId, (ignored, lastFireTime) -> {
            if (lastFireTime == null || now - lastFireTime >= minimumIntervalMs) {
                allowed.set(true);
                return now;
            }
            return lastFireTime;
        });

        return allowed.get();
    }

    public void clearRoom(int roomId) {
        lastFireAt.remove(roomId);
    }

    public void clearAll() {
        lastFireAt.clear();
    }
}
