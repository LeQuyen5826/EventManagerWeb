package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(@NotBlank(message = "Tên loại không được để trống") @Size(max = 100) String name) {}
