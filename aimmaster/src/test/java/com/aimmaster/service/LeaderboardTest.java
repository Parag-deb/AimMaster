package com.aimmaster.service;

import com.aimmaster.dto.LeaderboardEntry;
import com.aimmaster.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Leaderboard service.
 *
 * Verifies score tracking, thread-safety, and correct sorting of standings.
 */
class LeaderboardTest {

    private Leaderboard leaderboard;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        leaderboard = new Leaderboard();
        player1 = new Player("shooter1", "pass");
        // Use reflection to set the ID since it's protected for JPA
        setId(player1, 1);
        player2 = new Player("shooter2", "pass");
        setId(player2, 2);
    }

    private void setId(Player player, int id) {
        try {
            java.lang.reflect.Field field = Player.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(player, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void registerPlayer_initializesScoreToZero() {
        leaderboard.registerPlayer(1, player1);
        assertEquals(0, leaderboard.getScore(1, 1));
    }

    @Test
    void addScore_incrementsCorrectly() {
        leaderboard.registerPlayer(1, player1);
        int newScore = leaderboard.addScore(1, 1, 10);
        assertEquals(10, newScore);
        assertEquals(10, leaderboard.getScore(1, 1));
    }

    @Test
    void multipleAddScore_accumulates() {
        leaderboard.registerPlayer(1, player1);
        leaderboard.addScore(1, 1, 10);
        leaderboard.addScore(1, 1, 10);
        assertEquals(20, leaderboard.getScore(1, 1));
    }

    @Test
    void getStandings_sortsByScoreDescending() {
        leaderboard.registerPlayer(1, player1);
        leaderboard.registerPlayer(1, player2);
        leaderboard.addScore(1, 1, 10); // player1: 10
        leaderboard.addScore(1, 2, 30); // player2: 30

        List<LeaderboardEntry> standings = leaderboard.getStandings(1);
        assertEquals(2, standings.size());
        assertEquals(2, standings.get(0).playerId()); // player2 first (30 points)
        assertEquals(30, standings.get(0).score());
        assertEquals(1, standings.get(1).playerId()); // player1 second (10 points)
        assertEquals(10, standings.get(1).score());
    }

    @Test
    void unregisterPlayer_removesFromStandings() {
        leaderboard.registerPlayer(1, player1);
        leaderboard.addScore(1, 1, 10);
        leaderboard.unregisterPlayer(1, 1);

        List<LeaderboardEntry> standings = leaderboard.getStandings(1);
        assertTrue(standings.isEmpty());
    }

    @Test
    void clearRoom_removesAllData() {
        leaderboard.registerPlayer(1, player1);
        leaderboard.registerPlayer(1, player2);
        leaderboard.addScore(1, 1, 10);
        leaderboard.addScore(1, 2, 20);

        leaderboard.clearRoom(1);

        assertTrue(leaderboard.getStandings(1).isEmpty());
        assertEquals(0, leaderboard.getScore(1, 1));
    }

    @Test
    void concurrentScoreAdditions_areThreadSafe() throws InterruptedException {
        leaderboard.registerPlayer(1, player1);
        int numThreads = 100;
        int incrementsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        leaderboard.addScore(1, 1, 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        int expectedScore = numThreads * incrementsPerThread;
        assertEquals(expectedScore, leaderboard.getScore(1, 1),
                "Concurrent score additions should be thread-safe");
    }
}