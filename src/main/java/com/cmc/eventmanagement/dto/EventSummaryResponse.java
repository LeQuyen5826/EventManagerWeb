package com.cmc.eventmanagement.dto;

import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.EventStatus;

import java.time.LocalDateTime;

public record EventSummaryResponse(Long id, String title, String categoryName, String venueName,
                                   Integer capacity, LocalDateTime startAt, EventStatus status) {
    public static EventSummaryResponse from(Event e) {
        return new EventSummaryResponse(e.getId(), e.getTitle(), e.getCategory().getName(),
                e.getVenue().getName(), e.getCapacity(), e.getStartAt(), e.getStatus());
    }
}
