package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.LoginRequest;
import com.cmc.eventmanagement.dto.LoginResponse;
import com.cmc.eventmanagement.dto.RegisterUserRequest;
import com.cmc.eventmanagement.dto.UserResponse;
import com.cmc.eventmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
