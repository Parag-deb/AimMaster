package com.aimmaster.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TargetTest {

    @Test
    void targetStartsAliveAtGivenPosition() {
        Position pos = new Position(10.0, 20.0);
        Target target = new Target(1, TargetType.BIRD, pos, 2.0, 0.0);

        assertTrue(target.isAlive());
        assertEquals(1, target.getId());
        assertEquals(TargetType.BIRD, target.getType());
        assertEquals(10.0, target.getPosition().getX());
        assertEquals(20.0, target.getPosition().getY());
        assertEquals(2.0, target.getVelocityX());
        assertEquals(0.0, target.getVelocityY());
    }

    @Test
    void moveUpdatesPositionByVelocity() {
        Position pos = new Position(0.0, 0.0);
        Target target = new Target(1, TargetType.BALLOON, pos, 0.0, -1.0);

        target.move();

        assertEquals(0.0, target.getPosition().getX());
        assertEquals(-1.0, target.getPosition().getY());
    }

    @Test
    void hitMarksTargetDead() {
        Position pos = new Position(5.0, 5.0);
        Target target = new Target(1, TargetType.FRUIT, pos, 0.0, 3.0);

        target.hit();

        assertFalse(target.isAlive());
    }

    @Test
    void moveDoesNothingAfterHit() {
        Position pos = new Position(5.0, 5.0);
        Target target = new Target(1, TargetType.BIRD, pos, 2.0, 0.0);

        target.hit();
        target.move();

        assertEquals(5.0, target.getPosition().getX());
        assertEquals(5.0, target.getPosition().getY());
    }
}
