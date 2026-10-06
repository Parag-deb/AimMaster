package com.aimmaster.service;

import com.aimmaster.dto.SettingsUpdateRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.BadRequestException;
import com.aimmaster.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsServiceTest {

    private PlayerRepository playerRepository;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() throws Exception {
        playerRepository = mock(PlayerRepository.class);
        settingsService = new SettingsService(playerRepository);

        Player current = new Player("current", "pw");
        java.lang.reflect.Field idField = Player.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(current, 1);

        var auth = new TestingAuthenticationToken(current, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void updateSettings_changesTheme() throws Exception {
        Player player = new Player("demo", "pw");
        java.lang.reflect.Field idField = Player.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(player, 1);

        when(playerRepository.findById(1)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        var updated = settingsService.updateSettings(new SettingsUpdateRequest("dark"));
        assertEquals("dark", updated.getTheme());
    }

    @Test
    void updateSettings_rejectsInvalidTheme() throws Exception {
        Player player = new Player("current", "pw");
        java.lang.reflect.Field idField = Player.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(player, 1);
        when(playerRepository.findById(1)).thenReturn(Optional.of(player));

        assertThrows(BadRequestException.class,
                () -> settingsService.updateSettings(new SettingsUpdateRequest("invalid")));
    }
}
