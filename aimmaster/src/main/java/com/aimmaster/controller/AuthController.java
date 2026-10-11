package com.aimmaster.controller;

import com.aimmaster.dto.AuthResponse;
import com.aimmaster.dto.LoginRequest;
import com.aimmaster.dto.RegisterRequest;
import com.aimmaster.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FR1: register / login. This controller is intentionally thin - it does no password
 * checking or token building itself, it just hands the validated request to AuthService
 * and returns whatever comes back (or lets GlobalExceptionHandler turn an exception into
 * a clean error response).
 *
 * "@RestController" = "this class handles HTTP requests and every method's return value
 * becomes the JSON response body" (instead of the name of an HTML page to render).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
