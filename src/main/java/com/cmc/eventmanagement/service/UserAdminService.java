package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.UserResponse;
import com.cmc.eventmanagement.entity.Role;
import com.cmc.eventmanagement.entity.User;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.RoleRepository;
import com.cmc.eventmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAdminService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AuditLogService auditLog;

    /** FR05 */
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepo.findAll(pageable).map(UserResponse::from);
    }

    /** Phân quyền: Admin gán lại vai trò cho người dùng. */
    @Transactional
    public UserResponse updateRoles(Long userId, Set<String> roleNames, Long actorId) {
        User u = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            roles.add(roleRepo.findByName(name.toUpperCase())
                    .orElseThrow(() -> new BusinessException("Vai trò không hợp lệ: " + name)));
        }
        u.setRoles(roles);
        auditLog.log(actorId, "UPDATE_ROLES", "user=" + userId + " roles=" + roleNames);
        return UserResponse.from(u);
    }
}
