package com.aimmaster.service;

import com.aimmaster.dto.ArenaStateBroadcast;
import com.aimmaster.dto.TargetState;
import com.aimmaster.entity.Room;
import com.aimmaster.model.Position;
import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class GameSession {

    private static final Logger log = LoggerFactory.getLogger(GameSession.class);

    private static final int TICK_INTERVAL_MS = 100;
    private static final double ARENA_WIDTH = 800;
    private static final double ARENA_HEIGHT = 600;
    private static final double SPAWN_MARGIN = 50;

    private final Room room;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong elapsedMs = new AtomicLong(0);
    private final AtomicInteger nextTargetId = new AtomicInteger(1);
    private ScheduledFuture<?> gameLoopFuture;

    private final ConcurrentHashMap<Integer, Target> targets = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final HitResolver hitResolver;

    public GameSession(Room room, SimpMessagingTemplate messagingTemplate, HitResolver hitResolver) {
        this.room = room;
        this.messagingTemplate = messagingTemplate;
        this.hitResolver = hitResolver;
        this.executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameSession-" + room.getCode());
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized boolean start() {
        if (running.get()) {
            log.warn("GameSession for room {} already running", room.getCode());
            return false;
        }

        log.info("Starting GameSession for room {} (id={})", room.getCode(), room.getId());
        running.set(true);
        elapsedMs.set(0);

        room.clearTargets();

        gameLoopFuture = executor.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        return true;
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }

        log.info("Stopping GameSession for room {} (id={})", room.getCode(), room.getId());
        running.set(false);

        if (gameLoopFuture != null) {
            gameLoopFuture.cancel(false);
            gameLoopFuture = null;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        targets.clear();
        room.clearTargets();
    }

    private void tick() {
        if (!running.get()) {
            return;
        }

        long now = elapsedMs.addAndGet(TICK_INTERVAL_MS);

        if (now >= room.getSettings().getDurationSeconds() * 1000L) {
            log.info("Match duration reached for room {}", room.getCode());
            stop();
            return;
        }

        double difficultyMultiplier = 1.0 + (now / 1000.0) / 60.0 * 0.5;
        double spawnRate = calculateSpawnRate(difficultyMultiplier);

        moveTargets();
        despawnOffscreenTargets();
        maybeSpawnTarget(spawnRate, difficultyMultiplier);
        broadcastArenaState();
    }

    private void moveTargets() {
        for (Target target : targets.values()) {
            if (target.isAlive()) {
                target.move();
            }
        }
    }

    private void despawnOffscreenTargets() {
        List<Integer> toRemove = new ArrayList<>();
        for (Target target : targets.values()) {
            Position pos = target.getPosition();
            if (pos.getX() < -SPAWN_MARGIN || pos.getX() > ARENA_WIDTH + SPAWN_MARGIN ||
                pos.getY() < -SPAWN_MARGIN || pos.getY() > ARENA_HEIGHT + SPAWN_MARGIN) {
                toRemove.add(target.getId());
            }
        }
        for (Integer id : toRemove) {
            Target removed = targets.remove(id);
            if (removed != null) {
                room.removeTarget(removed);
                log.debug("Despawned target {} from room {}", id, room.getCode());
            }
        }
    }

    private void maybeSpawnTarget(double spawnRate, double difficultyMultiplier) {
        if (random.nextDouble() < spawnRate) {
            spawnTarget(difficultyMultiplier);
        }
    }

    private void spawnTarget(double difficultyMultiplier) {
        TargetType type = room.getSettings().getDifficulty();
        int targetId = nextTargetId.getAndIncrement();

        double x, y, vx, vy;

        switch (type) {
            case BIRD:
                y = SPAWN_MARGIN + random.nextDouble() * (ARENA_HEIGHT - 2 * SPAWN_MARGIN);
                x = -SPAWN_MARGIN;
                vx = (50 + random.nextDouble() * 30) * difficultyMultiplier;
                vy = (random.nextDouble() - 0.5) * 20;
                break;
            case BALLOON:
                x = SPAWN_MARGIN + random.nextDouble() * (ARENA_WIDTH - 2 * SPAWN_MARGIN);
                y = ARENA_HEIGHT + SPAWN_MARGIN;
                vx = (random.nextDouble() - 0.5) * 10;
                vy = -(20 + random.nextDouble() * 15) * difficultyMultiplier;
                break;
            case FRUIT:
                x = SPAWN_MARGIN + random.nextDouble() * (ARENA_WIDTH - 2 * SPAWN_MARGIN);
                y = -SPAWN_MARGIN;
                vx = (random.nextDouble() - 0.5) * 15;
                vy = (30 + random.nextDouble() * 20) * difficultyMultiplier;
                break;
            default:
                return;
        }

        Position position = new Position(x, y);
        Target target = new Target(targetId, type, position, vx, vy);
        targets.put(targetId, target);
        room.addTarget(target);

        log.debug("Spawned target {} ({}) in room {}", targetId, type, room.getCode());
    }

    private double calculateSpawnRate(double difficultyMultiplier) {
        double baseRate = 0.02;
        return Math.min(baseRate * difficultyMultiplier, 0.15);
    }

    private void broadcastArenaState() {
        List<TargetState> targetStates = new ArrayList<>();
        for (Target target : targets.values()) {
            if (target.isAlive()) {
                Position pos = target.getPosition();
                targetStates.add(new TargetState(
                    target.getId(),
                    target.getType().name(),
                    pos.getX(),
                    pos.getY(),
                    target.getVelocityX(),
                    target.getVelocityY()
                ));
            }
        }

        ArenaStateBroadcast broadcast = new ArenaStateBroadcast(
            room.getId(),
            targetStates,
            elapsedMs.get(),
            running.get()
        );

        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), broadcast);
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getElapsedMs() {
        return elapsedMs.get();
    }

    public int getActiveTargetCount() {
        return (int) targets.values().stream().filter(Target::isAlive).count();
    }

    public Room getRoom() {
        return room;
    }

    public boolean handleHit(int targetId, int playerId) {
        Target target = targets.get(targetId);
        if (target != null) {
            boolean hit = hitResolver.resolveHit(target, playerId);
            if (hit) {
                room.removeTarget(target);
                targets.remove(targetId);
                log.debug("Target {} hit by player {} in room {}", targetId, playerId, room.getCode());
            }
            return hit;
        }
        return false;
    }
}