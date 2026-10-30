package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank(message = "Tên sự kiện không được để trống") @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull Long categoryId,
        @NotNull Long venueId,
        @NotNull @Min(value = 1, message = "Sức chứa phải lớn hơn 0") @Max(10000) Integer capacity,
        @NotNull @Future(message = "Thời gian bắt đầu phải ở tương lai") LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull LocalDateTime registrationOpenAt,
        @NotNull LocalDateTime registrationCloseAt) {}
