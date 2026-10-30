package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.repository.EventRepository;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** BR07: nhân viên chỉ check-in cho sự kiện được phân công (organizer của sự kiện cũng được). */
@Component
@RequiredArgsConstructor
public class EventStaffGuard {
    private final EventRepository eventRepo;

    public void assertAssigned(Long eventId, AppUserDetails me) {
        if (me.isAdmin()) return;
        if (eventRepo.countOrganizerOrStaff(eventId, me.getId()) == 0) {
            throw new AccessDeniedException("Bạn không được phân công cho sự kiện này");
        }
    }
}
