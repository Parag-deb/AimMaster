package com.aimmaster.service;

import com.aimmaster.entity.Player;
import com.aimmaster.exception.InvalidCredentialsException;
import com.aimmaster.exception.UsernameAlreadyExistsException;
import com.aimmaster.repository.PlayerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account management: viewing/editing the current player's profile and settings.
 * Kept separate from AuthService (which owns login/registration) so each class
 * has exactly one reason to change.
 */
@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Player getCurrentPlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Player not found"));
    }

    @Transactional
    public Player updateProfile(String currentUsername, String newUsername, String theme) {
        Player player = playerRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new InvalidCredentialsException("Player not found"));

        if (!currentUsername.equals(newUsername) && playerRepository.existsByUsername(newUsername)) {
            throw new UsernameAlreadyExistsException(newUsername);
        }

        player.setUsername(newUsername);
        if (theme != null) {
            player.setTheme(theme);
        }

        return playerRepository.save(player);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Player not found"));

        if (!passwordEncoder.matches(currentPassword, player.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        player.setPasswordHash(passwordEncoder.encode(newPassword));
        playerRepository.save(player);
    }
}
