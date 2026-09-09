package com.aimmaster.service;

import com.aimmaster.entity.Room;
import com.aimmaster.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {

    private static final Logger log = LoggerFactory.getLogger(GameSessionManager.class);

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Integer, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSessionManager(RoomRepository roomRepository, SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messagingTemplate = messagingTemplate;
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

        GameSession session = new GameSession(room, messagingTemplate);
        sessions.put(roomId, session);
        boolean started = session.start();
        if (started) {
            log.info("Match started for room {} (id={})", room.getCode(), roomId);
        }
        return started;
    }

    public synchronized void stopMatch(Integer roomId) {
        GameSession session = sessions.remove(roomId);
        if (session != null) {
            session.stop();
            log.info("Match stopped for room {}", roomId);
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
            session.handleHit(targetId, playerId);
        }
    }

    public void cleanupRoom(Integer roomId) {
        stopMatch(roomId);
    }
}