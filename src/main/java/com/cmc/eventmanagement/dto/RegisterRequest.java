package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.Size;

/** Thông tin bổ sung khi đăng ký tham gia (FR28). */
public record RegisterRequest(@Size(max = 30) String studentId, @Size(max = 100) String organization) {}
