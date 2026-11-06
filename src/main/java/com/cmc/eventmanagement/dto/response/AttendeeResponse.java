package com.cmc.eventmanagement.dto.response;

import com.cmc.eventmanagement.entity.RegistrationStatus;

import java.time.LocalDateTime;

/** Dòng trong danh sách đăng ký của sự kiện (FR35). */
public record AttendeeResponse(Long registrationId, Long userId, String fullName, String email,
                               String studentId, String organization,
                               RegistrationStatus status, LocalDateTime registeredAt) {}
