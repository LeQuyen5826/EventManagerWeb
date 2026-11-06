package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VenueRequest(
        @NotBlank(message = "Tên địa điểm không được để trống") @Size(max = 150) String name,
        @Size(max = 255) String address,
        @Min(value = 1, message = "Sức chứa tối đa phải lớn hơn 0") int maxCapacity) {}
