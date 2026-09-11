package com.aimmaster.service;

import com.aimmaster.entity.MatchRecord;
import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.repository.MatchRecordRepository;
import com.aimmaster.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {

    private static final Logger log = LoggerFactory.getLogger(GameSessionManager.class);

    private final RoomRepository roomRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final HitResolver hitResolver;
    private final Leaderboard leaderboard;
    private final Map<Integer, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSessionManager(RoomRepository roomRepository, MatchRecordRepository matchRecordRepository,
                              SimpMessagingTemplate messagingTemplate,
                              HitResolver hitResolver, Leaderboard leaderboard) {
        this.roomRepository = roomRepository;
        this.matchRecordRepository = matchRecordRepository;
        this.messagingTemplate = messagingTemplate;
        this.hitResolver = hitResolver;
        this.leaderboard = leaderboard;
    }

    public synchronized boolean startMatch(Integer roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.warn("Cannot start match: room {} not found", roomId);
            return false;
        }

        if (sessions.containsKey(roomId)) {
            GameSession existing = sessions.get(roomId);
            if (existing.isRunning()) {
                log.warn("Match already running for room {}", roomId);
                return false;
            } else {
                sessions.remove(roomId);
            }
        }

        GameSession session = new GameSession(room, messagingTemplate, hitResolver);
        sessions.put(roomId, session);
        boolean started = session.start();
        if (started) {
            // Register all players in the room for leaderboard tracking
            for (Player player : room.getPlayers()) {
                leaderboard.registerPlayer(roomId, player);
            }
            log.info("Match started for room {} (id={})", room.getCode(), roomId);
        }
        return started;
    }

    public synchronized void stopMatch(Integer roomId) {
        GameSession session = sessions.remove(roomId);
        if (session != null) {
            // Persist match records before stopping
            persistMatchRecords(roomId, session);
            session.stop();
            leaderboard.clearRoom(roomId);
            log.info("Match stopped for room {}", roomId);
        }
    }

    private void persistMatchRecords(Integer roomId, GameSession session) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.warn("Cannot persist match records: room {} not found", roomId);
            return;
        }

        Instant timestamp = Instant.now();
        for (Player player : room.getPlayers()) {
            int playerId = player.getId();
            int score = leaderboard.getScore(roomId, playerId);
            int shots = session.getShotsFired(playerId);
            int hitCount = session.getHits(playerId);
            double accuracy = session.getAccuracy(playerId);

            MatchRecord record = new MatchRecord(
                    playerId, roomId, score, accuracy, shots, hitCount, timestamp
            );
            matchRecordRepository.save(record);
            log.info("Saved MatchRecord for player {} in room {}: score={}, accuracy={:.2%}, shots={}, hits={}",
                    playerId, roomId, score, accuracy, shots, hitCount);
        }
    }

    public GameSession getSession(Integer roomId) {
        return sessions.get(roomId);
    }

    public boolean isMatchRunning(Integer roomId) {
        GameSession session = sessions.get(roomId);
        return session != null && session.isRunning();
    }

    public void handleHit(Integer roomId, int targetId, int playerId) {
        GameSession session = sessions.get(roomId);
        if (session != null) {
            boolean hit = session.handleHit(targetId, playerId);
            if (hit) {
                // Award points and broadcast updated leaderboard
                int newScore = leaderboard.addScore(roomId, playerId, 10);
                broadcastHitResult(roomId, targetId, playerId, newScore);
                broadcastLeaderboard(roomId);
            }
        }
    }

    private void broadcastHitResult(Integer roomId, int targetId, int playerId, int newScore) {
        com.aimmaster.dto.FireBroadcast broadcast = new com.aimmaster.dto.FireBroadcast(
                targetId, playerId, newScore, System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, broadcast);
    }

    private void broadcastLeaderboard(Integer roomId) {
        List<com.aimmaster.dto.LeaderboardEntry> standings = leaderboard.getStandings(roomId);
        com.aimmaster.dto.LeaderboardBroadcast broadcast = new com.aimmaster.dto.LeaderboardBroadcast(
                standings, System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, broadcast);
    }

    public void cleanupRoom(Integer roomId) {
        stopMatch(roomId);
    }
}