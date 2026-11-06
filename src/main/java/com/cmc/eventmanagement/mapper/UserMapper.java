package com.cmc.eventmanagement.mapper;

import com.cmc.eventmanagement.dto.response.UserResponse;
import com.cmc.eventmanagement.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAvatarUrl(),
                u.isEnabled(), u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()));
    }
}
