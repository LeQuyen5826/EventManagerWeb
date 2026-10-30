package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.*;

public record RegisterUserRequest(
        @NotBlank(message = "Họ tên không được để trống") @Size(max = 100) String fullName,
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email,
        @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ") String phone,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 64, message = "Mật khẩu từ 8 đến 64 ký tự") String password) {}
