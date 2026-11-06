package com.cmc.eventmanagement.dto.response;

import com.cmc.eventmanagement.entity.RegistrationStatus;

public record RegistrationResponse(Long id, Long eventId, String eventTitle,
                                   RegistrationStatus status, String qrToken) {}
