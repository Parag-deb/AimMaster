package com.aimmaster.entity;

import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import com.aimmaster.model.Position;
import com.aimmaster.model.Target;
import com.aimmaster.model.TargetType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void roomContainsHostOnCreation() {
        Player host = new Player("hostUser", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 4);
        Room room = new Room("ABCD", host, settings);

        assertNull(room.getId());
        assertEquals("ABCD", room.getCode());
        assertSame(host, room.getHost());
        assertEquals(1, room.getPlayers().size());
        assertSame(host, room.getPlayers().get(0));
        assertTrue(room.getTargets().isEmpty());
        assertEquals(TargetType.BIRD, room.getSettings().getDifficulty());
        assertEquals(60, room.getSettings().getDurationSeconds());
        assertEquals(4, room.getSettings().getMaxPlayers());
    }

    @Test
    void addPlayerIncreasesPlayerCount() {
        Player host = new Player("host", "pass");
        Player player = new Player("shooter", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 4);
        Room room = new Room("ABCD", host, settings);

        boolean added = room.addPlayer(player);

        assertTrue(added);
        assertEquals(2, room.getPlayers().size());
    }

    @Test
    void addPlayerFailsWhenRoomIsFull() {
        Player host = new Player("host", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 1);
        Room room = new Room("ABCD", host, settings);
        Player extra = new Player("extra", "pass");

        boolean added = room.addPlayer(extra);

        assertFalse(added);
        assertEquals(1, room.getPlayers().size());
    }

    @Test
    void addPlayerFailsWhenAlreadyInRoom() {
        Player host = new Player("host", "pass");
        Player player = new Player("shooter", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 4);
        Room room = new Room("ABCD", host, settings);

        room.addPlayer(player);
        boolean addedAgain = room.addPlayer(player);

        assertFalse(addedAgain);
        assertEquals(2, room.getPlayers().size());
    }

    @Test
    void removePlayerDecreasesCount() {
        Player host = new Player("host", "pass");
        Player player = new Player("shooter", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 4);
        Room room = new Room("ABCD", host, settings);

        room.addPlayer(player);
        boolean removed = room.removePlayer(player);

        assertTrue(removed);
        assertEquals(1, room.getPlayers().size());
    }

    @Test
    void addTargetIncreasesTargetCount() {
        Player host = new Player("host", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 4);
        Room room = new Room("ABCD", host, settings);
        Position pos = new Position(100.0, 200.0);
        Target target = new Target(1, TargetType.BIRD, pos, 2.0, 0.0);

        room.addTarget(target);

        assertEquals(1, room.getTargets().size());
        assertSame(target, room.getTargets().get(0));
    }

    @Test
    void removeTargetDecreasesTargetCount() {
        Player host = new Player("host", "pass");
        RoomSettings settings = new RoomSettings(TargetType.BIRD, 60, 4);
        Room room = new Room("ABCD", host, settings);
        Position pos = new Position(100.0, 200.0);
        Target target = new Target(1, TargetType.BIRD, pos, 2.0, 0.0);

        room.addTarget(target);
        room.removeTarget(target);

        assertTrue(room.getTargets().isEmpty());
    }
}
