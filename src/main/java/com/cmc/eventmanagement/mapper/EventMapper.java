package com.cmc.eventmanagement.mapper;

import com.cmc.eventmanagement.dto.response.EventResponse;
import com.cmc.eventmanagement.dto.response.EventSummaryResponse;
import com.cmc.eventmanagement.dto.response.SessionResponse;
import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.Session;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class EventMapper {

    public EventSummaryResponse toSummary(Event e, long seatsTaken) {
        return new EventSummaryResponse(e.getId(), e.getTitle(), e.getCoverImageUrl(),
                e.getCategory().getName(), e.getVenue().getName(), e.getStartAt(), e.getEndAt(),
                e.getCapacity(), Math.max(0, e.getCapacity() - seatsTaken), e.getStatus());
    }

    public EventResponse toResponse(Event e, long seatsTaken) {
        List<SessionResponse> sessions = e.getSessions().stream()
                .sorted(Comparator.comparing(Session::getStartAt))
                .map(this::toSession).toList();
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(), e.getCoverImageUrl(),
                e.getCategory().getId(), e.getCategory().getName(),
                e.getVenue().getId(), e.getVenue().getName(), e.getVenue().getAddress(),
                e.getOrganizer().getId(), e.getOrganizer().getFullName(),
                e.getCapacity(), Math.max(0, e.getCapacity() - seatsTaken),
                e.getStartAt(), e.getEndAt(), e.getRegistrationOpenAt(), e.getRegistrationCloseAt(),
                e.getStatus(), e.getCancelReason(), e.getRejectReason(), sessions);
    }

    public SessionResponse toSession(Session s) {
        return new SessionResponse(s.getId(), s.getTitle(), s.getStartAt(), s.getEndAt(),
                s.getRoom(), s.getSpeakerName(), s.getDescription());
    }
}
