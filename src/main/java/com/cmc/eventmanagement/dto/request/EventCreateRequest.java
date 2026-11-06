package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** Dùng cho cả tạo mới (FR09) và cập nhật (FR10). */
public record EventCreateRequest(
        @NotBlank(message = "Tên sự kiện không được để trống") @Size(max = 200) String title,
        @Size(max = 2000) String description,
        String coverImageUrl,
        @NotNull(message = "Loại sự kiện không được để trống") Long categoryId,
        @NotNull(message = "Địa điểm không được để trống") Long venueId,
        @NotNull @Min(value = 1, message = "Sức chứa phải lớn hơn 0") @Max(10000) Integer capacity,
        @NotNull @Future(message = "Thời gian bắt đầu phải ở tương lai") LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull LocalDateTime registrationOpenAt,
        @NotNull LocalDateTime registrationCloseAt) {}
