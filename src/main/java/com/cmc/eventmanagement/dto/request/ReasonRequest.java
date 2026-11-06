package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lý do hủy sự kiện (FR13) hoặc từ chối duyệt (FR12). */
public record ReasonRequest(@NotBlank(message = "Vui lòng nhập lý do") @Size(max = 255) String reason) {}
