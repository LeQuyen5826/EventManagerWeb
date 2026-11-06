package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, max = 64, message = "Mật khẩu từ 8 đến 64 ký tự") String newPassword) {}
