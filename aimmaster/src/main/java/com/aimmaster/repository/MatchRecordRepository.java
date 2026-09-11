package com.aimmaster.repository;

import com.aimmaster.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    List<MatchRecord> findByPlayerIdOrderByTimestampDesc(Integer playerId);

    List<MatchRecord> findByRoomIdOrderByScoreDesc(Integer roomId);

    @Query("SELECT m FROM MatchRecord m WHERE m.playerId = :playerId ORDER BY m.timestamp DESC")
    List<MatchRecord> findRecentByPlayerId(Integer playerId, org.springframework.data.domain.Pageable pageable);
}