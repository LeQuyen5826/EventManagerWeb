package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.NotNull;

public record AssignStaffRequest(@NotNull(message = "userId không được để trống") Long userId) {}
