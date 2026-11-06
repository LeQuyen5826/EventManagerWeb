package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SessionRequest(
        @NotBlank(message = "Tiêu đề phiên không được để trống") @Size(max = 200) String title,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @Size(max = 100) String room,
        @Size(max = 100) String speakerName,
        @Size(max = 1000) String description) {}
