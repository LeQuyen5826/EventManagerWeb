package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignStaffRequest(@NotNull(message = "userId không được để trống") Long userId) {}
