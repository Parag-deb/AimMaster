package com.aimmaster.model;

import java.util.Objects;

/**
 * A single target the player can shoot.
 *
 * Responsibilities:
 * - Knows its type (bird, balloon, or fruit).
 * - Tracks position and movement velocity.
 * - Knows whether it is still alive (has not been hit yet).
 *
 * The target does not decide when to spawn or despawn —
 * Room/GameSession manages the lifecycle.
 */
public class Target {
    private final int id;
    private final TargetType type;
    private Position position;
    private final double velocityX;
    private final double velocityY;
    private boolean alive;

    public Target(int id, TargetType type, Position position, double velocityX, double velocityY) {
        this.id = id;
        this.type = Objects.requireNonNull(type);
        this.position = Objects.requireNonNull(position);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.alive = true;
    }

    public int getId() {
        return id;
    }

    public TargetType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public boolean isAlive() {
        return alive;
    }

    public void move() {
        if (!alive) {
            return;
        }
        position = new Position(
            position.getX() + velocityX,
            position.getY() + velocityY
        );
    }

    public void hit() {
        alive = false;
    }
}
