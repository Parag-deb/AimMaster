package com.aimmaster.service;

import com.aimmaster.dto.AuthResponse;
import com.aimmaster.dto.LoginRequest;
import com.aimmaster.dto.RegisterRequest;
import com.aimmaster.entity.Player;
import com.aimmaster.exception.InvalidCredentialsException;
import com.aimmaster.exception.UsernameAlreadyExistsException;
import com.aimmaster.repository.PlayerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for FR1 (register/login). Controllers stay "thin" - all they do is
 * call one method here and return the result; every actual decision (is the username
 * taken? does the password match?) lives in this one place.
 */
@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        // Never save the raw password - only its one-way hash. BCrypt is the industry-standard
        // choice here: even if the database leaks, the original passwords can't be recovered.
        String hashed = passwordEncoder.encode(request.getPassword());
        Player player = new Player(request.getUsername(), hashed);
        playerRepository.save(player);

        String token = jwtService.generateToken(player.getUsername());
        return new AuthResponse(token, player.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        Player player = playerRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), player.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(player.getUsername());
        return new AuthResponse(token, player.getUsername());
    }
}
