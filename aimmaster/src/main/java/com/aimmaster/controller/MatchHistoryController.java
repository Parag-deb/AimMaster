package com.aimmaster.controller;

import com.aimmaster.dto.MatchRecordResponse;
import com.aimmaster.entity.MatchRecord;
import com.aimmaster.repository.MatchRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class MatchHistoryController {

    private final MatchRecordRepository matchRecordRepository;

    public MatchHistoryController(MatchRecordRepository matchRecordRepository) {
        this.matchRecordRepository = matchRecordRepository;
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<MatchRecordResponse>> getPlayerHistory(
            @PathVariable Integer id,
            @AuthenticationPrincipal com.aimmaster.entity.Player player) {

        // Players can only view their own history
        if (!player.getId().equals(id)) {
            return ResponseEntity.status(403).build();
        }

        List<MatchRecord> records = matchRecordRepository.findByPlayerIdOrderByTimestampDesc(id);
        List<MatchRecordResponse> response = records.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private MatchRecordResponse toResponse(MatchRecord record) {
        return new MatchRecordResponse(
                record.getId(),
                record.getPlayerId(),
                record.getRoomId(),
                record.getScore(),
                record.getAccuracy(),
                record.getShotsFired(),
                record.getHits(),
                record.getTimestamp()
        );
    }
}