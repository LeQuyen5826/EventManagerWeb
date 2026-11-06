package com.cmc.eventmanagement.dto.response;

import com.cmc.eventmanagement.entity.EventStatus;

import java.time.LocalDateTime;

public record EventSummaryResponse(Long id, String title, String coverImageUrl, String categoryName,
                                   String venueName, LocalDateTime startAt, LocalDateTime endAt,
                                   int capacity, long availableSeats, EventStatus status) {}
