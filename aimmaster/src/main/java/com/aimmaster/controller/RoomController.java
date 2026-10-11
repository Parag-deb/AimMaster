package com.aimmaster.controller;

import com.aimmaster.dto.CreateRoomRequest;
import com.aimmaster.dto.LeaderboardEntry;
import com.aimmaster.dto.LiveMatchStateResponse;
import com.aimmaster.dto.MatchStatusResponse;
import com.aimmaster.dto.RoomResponse;
import com.aimmaster.service.MatchRecordService;
import com.aimmaster.service.MatchService;
import com.aimmaster.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FR2 / FR9: create a room, join a room by code.
 * FR10 (Sprint 4): host starts/stops a match. Room CRUD goes through RoomService,
 * match lifecycle goes through MatchService - this controller just stays a thin
 * router to whichever one fits the request.
 *
 * "Authentication authentication" is filled in automatically by Spring Security from
 * whatever JwtAuthenticationFilter put in the SecurityContext - authentication.getName()
 * gives us the calling player's username, straight from their verified JWT.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final MatchService matchService;
    private final MatchRecordService matchRecordService;

    public RoomController(RoomService roomService, MatchService matchService,
                           MatchRecordService matchRecordService) {
        this.roomService = roomService;
        this.matchService = matchService;
        this.matchRecordService = matchRecordService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request,
                                                     Authentication authentication) {
        RoomResponse response = roomService.createRoom(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Sprint 9 (spectator mode): plain room info, no side effects - unlike POST
     * .../join, this never adds the caller to the room or fails when the room is
     * full. The frontend's "Spectate" flow calls this instead of join.
     */
    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getRoom(code));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String code, Authentication authentication) {
        RoomResponse response = roomService.joinRoom(authentication.getName(), code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<MatchStatusResponse> startMatch(@PathVariable String code, Authentication authentication) {
        MatchStatusResponse response = matchService.startMatch(code, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/stop")
    public ResponseEntity<MatchStatusResponse> stopMatch(@PathVariable String code, Authentication authentication) {
        MatchStatusResponse response = matchService.stopMatch(code, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * FR6/Sprint 6: a non-real-time snapshot of a room's results, read from the
     * MatchRecordRepository (past, finished matches) - NOT from any live in-memory
     * Leaderboard, which only exists while a match is actually running. Same naming
     * choice as PlayerController: "{code}" here, not the plan's literal "{id}", to
     * stay consistent with every other /api/rooms/** endpoint.
     */
    @GetMapping("/{code}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@PathVariable String code) {
        return ResponseEntity.ok(matchRecordService.getLeaderboardSnapshot(code));
    }

    /**
     * Sprint 9 (reconnect handling): "catch me up" for a match that's running RIGHT
     * NOW - see LiveMatchStateResponse's javadoc. The frontend calls this immediately
     * after every successful WebSocket (re)connect, player or spectator alike.
     */
    @GetMapping("/{code}/live")
    public ResponseEntity<LiveMatchStateResponse> getLiveState(@PathVariable String code) {
        return ResponseEntity.ok(matchService.getLiveState(code));
    }
}
