package com.aimmaster.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void playerCreatedWithUsernameAndPassword() {
        Player player = new Player("testuser", "secret");

        assertNull(player.getId());
        assertEquals("testuser", player.getUsername());
        assertEquals("secret", player.getPassword());
    }
}
