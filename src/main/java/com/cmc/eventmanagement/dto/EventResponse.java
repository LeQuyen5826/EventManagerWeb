package com.cmc.eventmanagement.dto;

import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.EventStatus;

import java.time.LocalDateTime;

public record EventResponse(Long id, String title, String description,
                            Long categoryId, String categoryName, Long venueId, String venueName,
                            Integer capacity, LocalDateTime startAt, LocalDateTime endAt,
                            LocalDateTime registrationOpenAt, LocalDateTime registrationCloseAt,
                            EventStatus status, Long organizerId) {
    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(),
                e.getCategory().getId(), e.getCategory().getName(), e.getVenue().getId(), e.getVenue().getName(),
                e.getCapacity(), e.getStartAt(), e.getEndAt(), e.getRegistrationOpenAt(), e.getRegistrationCloseAt(),
                e.getStatus(), e.getOrganizer().getId());
    }
}
