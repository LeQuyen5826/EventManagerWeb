package com.cmc.eventmanagement.event;

/** Đăng ký được xác nhận (đăng ký còn chỗ, hoặc từ danh sách chờ lên). */
public record RegistrationConfirmedEvent(Long registrationId) {}
