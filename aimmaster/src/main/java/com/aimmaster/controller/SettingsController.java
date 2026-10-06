package com.aimmaster.controller;

import com.aimmaster.dto.SettingsUpdateRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<Player> getSettings() {
        Player settings = settingsService.getCurrentPlayerSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<Player> updateSettings(@RequestBody SettingsUpdateRequest request) {
        Player updated = settingsService.updateSettings(request);
        return ResponseEntity.ok(updated);
    }
}
