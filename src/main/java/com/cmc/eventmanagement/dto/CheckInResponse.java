package com.cmc.eventmanagement.dto;

import java.time.LocalDateTime;

public record CheckInResponse(Long registrationId, String eventTitle, String attendeeName,
                              String studentId, LocalDateTime checkedInAt) {}
