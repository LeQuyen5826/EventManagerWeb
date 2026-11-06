package com.cmc.eventmanagement.dto.request;

import com.cmc.eventmanagement.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(@NotEmpty(message = "Phải có ít nhất một vai trò") Set<RoleName> roles) {}
