package com.aimmaster.repository;

import com.aimmaster.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    /** A player's full match history, most recent first (FR8). */
    List<MatchRecord> findByPlayer_UsernameOrderByTimestampDesc(String username);

    /** Every recorded result for one room, highest score first - the non-real-time leaderboard snapshot. */
    List<MatchRecord> findByRoom_CodeOrderByScoreDesc(String roomCode);
}
