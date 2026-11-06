package com.cmc.eventmanagement.mapper;

import com.cmc.eventmanagement.dto.response.AttendeeResponse;
import com.cmc.eventmanagement.dto.response.RegistrationResponse;
import com.cmc.eventmanagement.entity.Registration;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {
    public RegistrationResponse toResponse(Registration r) {
        return new RegistrationResponse(r.getId(), r.getEvent().getId(), r.getEvent().getTitle(),
                r.getStatus(), r.getQrToken());
    }

    public AttendeeResponse toAttendee(Registration r) {
        return new AttendeeResponse(r.getId(), r.getUser().getId(), r.getUser().getFullName(),
                r.getUser().getEmail(), r.getStudentId(), r.getOrganization(), r.getStatus(), r.getCreatedAt());
    }
}
