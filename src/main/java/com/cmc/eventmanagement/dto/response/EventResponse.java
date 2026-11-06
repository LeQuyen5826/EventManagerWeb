package com.cmc.eventmanagement.dto.response;

import com.cmc.eventmanagement.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(Long id, String title, String description, String coverImageUrl,
                            Long categoryId, String categoryName, Long venueId, String venueName, String venueAddress,
                            Long organizerId, String organizerName,
                            int capacity, long availableSeats,
                            LocalDateTime startAt, LocalDateTime endAt,
                            LocalDateTime registrationOpenAt, LocalDateTime registrationCloseAt,
                            EventStatus status, String cancelReason, String rejectReason,
                            List<SessionResponse> sessions) {}
