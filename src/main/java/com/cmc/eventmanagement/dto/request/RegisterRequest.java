package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.Size;

/** Thông tin bổ sung khi đăng ký sự kiện (FR28). joinWaitlist = true: vào danh sách chờ nếu hết chỗ (FR32). */
public record RegisterRequest(
        @Size(max = 30) String studentId,
        @Size(max = 100) String organization,
        Boolean joinWaitlist) {}
