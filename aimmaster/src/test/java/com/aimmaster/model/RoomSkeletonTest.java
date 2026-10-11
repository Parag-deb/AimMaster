package com.aimmaster.model;

import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the core domain classes. These still need zero Spring context to run -
 * even though Player/Room are now @Entity classes (Sprint 2), they're still plain Java
 * objects until something actually saves them, so "new Room(...)" + assertions works
 * exactly like it did in Sprint 1. (Originally written against the Sprint 1 plain
 * model.Player/model.Room; updated in place when those became entity.Player/entity.Room
 * so we're not maintaining two parallel domain models.)
 */
class RoomSkeletonTest {

    @Test
    void hostAutomaticallyCountsAsAJoinedPlayer() {
        Player host = new Player("hostUser", "hashedPw");
        Room room = new Room("ABC123", host, Difficulty.MEDIUM, 120, 4);

        assertEquals(1, room.getPlayers().size());
        assertTrue(room.getPlayers().contains(host));
    }

    @Test
    void secondPlayerCanJoinAnExistingRoom() {
        Player host = new Player("host", "hashedPw");
        Room room = new Room("ROOM01", host, Difficulty.EASY, 60, 4);
        Player joiner = new Player("joiner", "hashedPw");

        room.addPlayer(joiner);

        assertEquals(2, room.getPlayers().size());
        assertTrue(room.getPlayers().contains(joiner));
    }

    @Test
    void roomRejectsPlayersOnceFull() {
        Player host = new Player("host", "hashedPw");
        Room room = new Room("FULL01", host, Difficulty.HARD, 90, 1); // host already fills it

        assertThrows(IllegalStateException.class, () -> room.addPlayer(new Player("tooMany", "hashedPw")));
    }

    @Test
    void spawningAndKillingATargetRemovesItLogically() {
        Target target = new Target(TargetType.BALLOON, 0, 0, 5, 0);
        assertTrue(target.isAlive());

        target.markDead();

        assertFalse(target.isAlive());
    }

    @Test
    void targetMovesAccordingToItsVelocity() {
        Target target = new Target(TargetType.BIRD, 0, 0, 10, -2);

        target.move(1.0); // 1 second of movement

        assertEquals(10.0, target.getX(), 0.001);
        assertEquals(-2.0, target.getY(), 0.001);
    }
}
