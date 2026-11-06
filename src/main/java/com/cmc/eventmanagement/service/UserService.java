package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.request.ChangePasswordRequest;
import com.cmc.eventmanagement.dto.request.UpdateProfileRequest;
import com.cmc.eventmanagement.dto.response.UserResponse;
import com.cmc.eventmanagement.entity.Role;
import com.cmc.eventmanagement.entity.RoleName;
import com.cmc.eventmanagement.entity.User;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.mapper.UserMapper;
import com.cmc.eventmanagement.repository.RoleRepository;
import com.cmc.eventmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final AuditLogService auditLog;

    // ---- FR04: hồ sơ cá nhân

    @Transactional(readOnly = true)
    public UserResponse profile(Long userId) { return mapper.toResponse(load(userId)); }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = load(userId);
        u.setFullName(req.fullName().trim());
        u.setPhone(req.phone());
        u.setAvatarUrl(req.avatarUrl());
        return mapper.toResponse(u);
    }

    /** FR03: đổi mật khẩu khi đã đăng nhập. */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User u = load(userId);
        if (!passwordEncoder.matches(req.oldPassword(), u.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    // ---- FR05, FR06: Admin quản lý người dùng

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return userRepo.search(kw, pageable).map(mapper::toResponse);
    }

    @Transactional
    public UserResponse setRoles(Long userId, Set<RoleName> names, Long actorId) {
        User u = load(userId);
        if (u.getId().equals(actorId) && !names.contains(RoleName.ADMIN)) {
            throw new BusinessException("Không thể tự gỡ vai trò Admin của chính mình");
        }
        Set<Role> roles = new HashSet<>();
        for (RoleName n : names) roles.add(roleRepo.findByName(n).orElseThrow());
        u.getRoles().clear();
        u.getRoles().addAll(roles);
        auditLog.log(actorId, "USER_SET_ROLES", "userId=" + userId + ", roles=" + names);
        return mapper.toResponse(u);
    }

    /** Khóa / mở khóa tài khoản. */
    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled, Long actorId) {
        User u = load(userId);
        if (u.getId().equals(actorId)) throw new BusinessException("Không thể tự khóa tài khoản của chính mình");
        u.setEnabled(enabled);
        if (enabled) { u.setFailedLoginCount(0); u.setLockedUntil(null); }
        auditLog.log(actorId, enabled ? "USER_UNLOCK" : "USER_LOCK", "userId=" + userId);
        return mapper.toResponse(u);
    }

    private User load(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
    }
}
