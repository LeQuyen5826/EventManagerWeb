package com.cmc.eventmanagement.dto;

import jakarta.validation.constraints.Size;

public record CancelEventRequest(@Size(max = 500) String reason) {}
