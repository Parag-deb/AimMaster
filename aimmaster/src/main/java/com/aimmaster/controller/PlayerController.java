package com.aimmaster.controller;

import com.aimmaster.dto.ChangePasswordRequest;
import com.aimmaster.dto.PlayerHistoryResponse;
import com.aimmaster.dto.PlayerInfoResponse;
import com.aimmaster.dto.UpdatePlayerRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.AccessForbiddenException;
import com.aimmaster.exception.InvalidCredentialsException;
import com.aimmaster.exception.UsernameAlreadyExistsException;
import com.aimmaster.service.MatchRecordService;
import com.aimmaster.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR8 extended + account management:
 * - GET /api/players/{username}/history — match history
 * - GET /api/players/me — current account info
 * - PUT /api/players/me — update profile (username, theme)
 * - PUT /api/players/me/password — change password
 */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final MatchRecordService matchRecordService;
    private final PlayerService playerService;

    public PlayerController(MatchRecordService matchRecordService, PlayerService playerService) {
        this.matchRecordService = matchRecordService;
        this.playerService = playerService;
    }

    @GetMapping("/{username}/history")
    public ResponseEntity<PlayerHistoryResponse> getHistory(@PathVariable String username,
                                                              Authentication authentication) {
        if (!authentication.getName().equals(username)) {
            throw new AccessForbiddenException("You can only view your own match history");
        }
        return ResponseEntity.ok(matchRecordService.getHistoryForPlayer(username));
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerInfoResponse> getMe(Authentication authentication) {
        Player player = playerService.getCurrentPlayer(authentication.getName());
        return ResponseEntity.ok(new PlayerInfoResponse(
                player.getUsername(),
                player.getTheme(),
                player.isSoundEnabled(),
                player.isMusicEnabled(),
                player.isShowHud(),
                player.getCreatedAt().toString()
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<PlayerInfoResponse> updateMe(@Valid @RequestBody UpdatePlayerRequest request,
                                                        Authentication authentication) {
        Player updated = playerService.updateProfile(
                authentication.getName(), request.username(), request.theme()
        );
        return ResponseEntity.ok(new PlayerInfoResponse(
                updated.getUsername(),
                updated.getTheme(),
                updated.isSoundEnabled(),
                updated.isMusicEnabled(),
                updated.isShowHud(),
                updated.getCreatedAt().toString()
        ));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            Authentication authentication) {
        try {
            playerService.changePassword(
                    authentication.getName(),
                    request.currentPassword(),
                    request.newPassword()
            );
            return ResponseEntity.ok().build();
        } catch (UsernameAlreadyExistsException | InvalidCredentialsException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    record ErrorResponse(String message) {
    }
}
