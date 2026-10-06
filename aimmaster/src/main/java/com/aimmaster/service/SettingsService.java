package com.aimmaster.service;

import com.aimmaster.dto.SettingsUpdateRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.BadRequestException;
import com.aimmaster.repository.PlayerRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final PlayerRepository playerRepository;

    public SettingsService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player getCurrentPlayerSettings() {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return playerRepository.findById(player.getId()).orElseThrow();
    }

    public Player updateSettings(SettingsUpdateRequest request) {
        Player player = (Player) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Player current = playerRepository.findById(player.getId()).orElseThrow();

        String theme = request.theme();
        if (theme != null) {
            if (!theme.equals("regular") && !theme.equals("dark") && !theme.equals("light")) {
                throw new BadRequestException("Invalid theme. Use: regular, dark, or light");
            }
            current.setTheme(theme);
        }

        return playerRepository.save(current);
    }
}
