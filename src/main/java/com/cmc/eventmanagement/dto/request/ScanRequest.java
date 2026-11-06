package com.cmc.eventmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(@NotBlank(message = "Mã QR không được để trống") String qrToken) {}
