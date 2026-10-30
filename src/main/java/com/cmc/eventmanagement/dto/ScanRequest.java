package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(@NotBlank(message = "qrToken không được để trống") String qrToken) {}
