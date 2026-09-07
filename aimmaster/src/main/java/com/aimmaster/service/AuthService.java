package com.aimmaster.service;

import com.aimmaster.config.JwtUtil;
import com.aimmaster.dto.AuthResponse;
import com.aimmaster.dto.LoginRequest;
import com.aimmaster.dto.RegisterRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.BadRequestException;
import com.aimmaster.repository.PlayerRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(PlayerRepository playerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken");
        }

        Player player = new Player(request.username(), passwordEncoder.encode(request.password()));
        Player saved = playerRepository.save(player);

        String token = jwtUtil.generateToken(saved.getId(), saved.getUsername());
        return new AuthResponse(token, saved.getUsername(), saved.getId());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            String username = auth.getName();
            Player player = playerRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Player not found after authentication"));

            String token = jwtUtil.generateToken(player.getId(), player.getUsername());
            return new AuthResponse(token, player.getUsername(), player.getId());
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
