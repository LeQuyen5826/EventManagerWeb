package com.cmc.eventmanagement.dto.response;

import java.time.LocalDateTime;

public record SessionResponse(Long id, String title, LocalDateTime startAt, LocalDateTime endAt,
                              String room, String speakerName, String description) {}
