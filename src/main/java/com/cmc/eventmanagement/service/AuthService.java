package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.request.LoginRequest;
import com.cmc.eventmanagement.dto.request.RegisterUserRequest;
import com.cmc.eventmanagement.dto.response.TokenResponse;
import com.cmc.eventmanagement.dto.response.UserResponse;
import com.cmc.eventmanagement.entity.Role;
import com.cmc.eventmanagement.entity.RoleName;
import com.cmc.eventmanagement.entity.User;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.mapper.UserMapper;
import com.cmc.eventmanagement.repository.RoleRepository;
import com.cmc.eventmanagement.repository.UserRepository;
import com.cmc.eventmanagement.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_FAILED = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    /** FR01: mật khẩu thô không bao giờ được lưu. */
    @Transactional
    public UserResponse register(RegisterUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) throw new ConflictException("Email đã được sử dụng");
        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        Role attendee = roleRepo.findByName(RoleName.ATTENDEE).orElseThrow();
        user.getRoles().add(attendee);
        return userMapper.toResponse(userRepo.save(user));
    }

    /** FR02: không để @Transactional ở đây để việc ghi số lần sai vẫn được lưu khi ném exception. */
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userRepo.findByEmail(email).orElse(null);
        if (user != null && user.isTemporarilyLocked(LocalDateTime.now())) {
            throw new LockedException("Tài khoản đang bị khóa tạm thời");
        }
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        } catch (BadCredentialsException ex) {
            if (user != null) recordFailure(user);
            throw ex;
        }
        if (user.getFailedLoginCount() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            userRepo.save(user);
        }
        return new TokenResponse(jwtService.generate(user), "Bearer", jwtService.expiresInSeconds());
    }

    private void recordFailure(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= MAX_FAILED) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setFailedLoginCount(0);
        }
        userRepo.save(user);
    }
}
