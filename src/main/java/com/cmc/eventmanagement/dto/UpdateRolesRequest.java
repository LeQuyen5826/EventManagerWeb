package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(@NotEmpty(message = "Phải có ít nhất một vai trò") Set<String> roles) {}
