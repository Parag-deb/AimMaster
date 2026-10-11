package com.aimmaster.service;

import com.aimmaster.dto.LeaderboardEntry;
import com.aimmaster.dto.MatchHistoryEntry;
import com.aimmaster.dto.PlayerHistoryResponse;
import com.aimmaster.entity.MatchRecord;
import com.aimmaster.entity.Player;
import com.aimmaster.entity.Room;
import com.aimmaster.exception.RoomNotFoundException;
import com.aimmaster.repository.MatchRecordRepository;
import com.aimmaster.repository.PlayerRepository;
import com.aimmaster.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Everything to do with the MatchRecord entity: writing one row per player when a
 * match ends (FR8), and reading them back for a player's history or a room's
 * historical leaderboard.
 */
@Service
public class MatchRecordService {

    private static final Logger log = LoggerFactory.getLogger(MatchRecordService.class);

    private final MatchRecordRepository matchRecordRepository;
    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;

    public MatchRecordService(MatchRecordRepository matchRecordRepository,
                               PlayerRepository playerRepository,
                               RoomRepository roomRepository) {
        this.matchRecordRepository = matchRecordRepository;
        this.playerRepository = playerRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Called exactly once by GameSessionManager the moment a match ends. Writes one
     * MatchRecord per player who was ever meaningfully part of this match (FR8) -
     * everyone in the room when it started, PLUS anyone who joined mid-match and
     * actually fired or scored (Sprint 8: see GameSession.getAllInvolvedUsernames()
     * for why the mid-match-joiner case matters). Even a player who never landed a
     * single hit still gets a (score 0, accuracy 0) row, because "played and scored
     * nothing" is still part of their history.
     *
     * We deliberately re-fetch the Room here via roomRepository instead of reusing
     * whatever Room reference GameSession has been holding onto: that reference came
     * from a database session that closed a long time ago (possibly minutes, for a
     * long match). Mixing an old, detached JPA entity into a brand-new save is a
     * classic, confusing bug - re-fetching fresh inside THIS method's own transaction
     * avoids it entirely.
     */
    @Transactional
    public void persistMatchRecords(GameSession session) {
        Room room = roomRepository.findByCode(session.getRoomCode())
                .orElseThrow(() -> new RoomNotFoundException(session.getRoomCode()));

        for (String username : session.getAllInvolvedUsernames()) {
            Player player = playerRepository.findByUsername(username).orElse(null);
            if (player == null) {
                // Shouldn't happen (a room member came from a real Player row in the first
                // place) - logged instead of thrown so one odd row can't stop the other
                // players' results from being saved.
                log.warn("Skipping match record for '{}' in room {} - player no longer exists",
                        username, room.getCode());
                continue;
            }

            int score = session.getLeaderboard().getScore(username);
            int attempts = session.getFireAttempts(username);
            double accuracy = attempts == 0 ? 0.0 : (double) score / attempts;

            matchRecordRepository.save(new MatchRecord(player, room, score, accuracy));
        }

        log.info("Persisted match records for room {} ({} players)", room.getCode(),
                session.getAllInvolvedUsernames().size());
    }

    @Transactional(readOnly = true)
    public PlayerHistoryResponse getHistoryForPlayer(String username) {
        List<MatchHistoryEntry> matches = matchRecordRepository
                .findByPlayer_UsernameOrderByTimestampDesc(username)
                .stream()
                .map(MatchHistoryEntry::from)
                .toList();

        double averageScore = matches.stream().mapToInt(MatchHistoryEntry::getScore).average().orElse(0.0);
        double averageAccuracy = matches.stream().mapToDouble(MatchHistoryEntry::getAccuracyPercent).average().orElse(0.0);

        return new PlayerHistoryResponse(username, matches.size(), averageScore, averageAccuracy, matches);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboardSnapshot(String roomCode) {
        if (!roomRepository.existsByCode(roomCode)) {
            throw new RoomNotFoundException(roomCode);
        }

        return matchRecordRepository.findByRoom_CodeOrderByScoreDesc(roomCode).stream()
                .map(record -> new LeaderboardEntry(record.getPlayer().getUsername(), record.getScore()))
                .toList();
    }
}
