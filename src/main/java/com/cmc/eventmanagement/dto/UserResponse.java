package com.cmc.eventmanagement.dto;

import com.cmc.eventmanagement.entity.Role;
import com.cmc.eventmanagement.entity.User;

import java.util.List;

public record UserResponse(Long id, String fullName, String email, String phone, boolean enabled, List<String> roles) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.isEnabled(),
                u.getRoles().stream().map(Role::getName).sorted().toList());
    }
}
