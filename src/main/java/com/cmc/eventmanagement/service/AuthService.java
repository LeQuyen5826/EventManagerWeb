package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.config.AppProperties;
import com.cmc.eventmanagement.dto.LoginRequest;
import com.cmc.eventmanagement.dto.LoginResponse;
import com.cmc.eventmanagement.dto.RegisterUserRequest;
import com.cmc.eventmanagement.dto.UserResponse;
import com.cmc.eventmanagement.entity.User;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.RoleRepository;
import com.cmc.eventmanagement.repository.UserRepository;
import com.cmc.eventmanagement.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppProperties props;

    /** FR01: đăng ký tài khoản, mặc định vai trò ATTENDEE. Mật khẩu thô không bao giờ được lưu. */
    @Transactional
    public UserResponse register(RegisterUserRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new ConflictException("Email đã được sử dụng");
        }
        User u = new User();
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        u.setPhone(req.phone());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.getRoles().add(roleRepo.findByName("ATTENDEE")
                .orElseThrow(() -> new IllegalStateException("Thiếu vai trò ATTENDEE trong CSDL")));
        return UserResponse.from(userRepo.save(u));
    }

    /** FR02: AuthenticationManager so khớp BCrypt; sai thì ném BadCredentialsException (401). */
    public LoginResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User u = userRepo.findByEmail(req.email()).orElseThrow();
        return new LoginResponse(jwtService.generate(u), "Bearer", props.jwt().expirationMinutes() * 60);
    }

    /** FR04: xem hồ sơ của chính mình. */
    @Transactional(readOnly = true)
    public UserResponse profile(Long userId) {
        return userRepo.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
    }
}
