package com.cmc.eventmanagement.dto.response;

import java.time.LocalDateTime;

public record CheckInResponse(Long registrationId, String fullName, String email, String avatarUrl,
                              String eventTitle, LocalDateTime checkedInAt, String checkedInBy) {}
