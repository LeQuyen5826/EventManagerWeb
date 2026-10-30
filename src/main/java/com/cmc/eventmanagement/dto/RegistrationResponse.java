package com.cmc.eventmanagement.dto;

import com.cmc.eventmanagement.entity.Registration;
import com.cmc.eventmanagement.entity.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

public record RegistrationResponse(Long id, Long eventId, String eventTitle,
                                   String attendeeName, String attendeeEmail,
                                   String studentId, String organization,
                                   RegistrationStatus status,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) String qrToken) {
    /** Dành cho chính người đăng ký: có kèm qrToken. */
    public static RegistrationResponse from(Registration r) {
        return build(r, r.getQrToken());
    }

    /** Dành cho organizer xem danh sách: không lộ qrToken của người khác. */
    public static RegistrationResponse forOrganizer(Registration r) {
        return build(r, null);
    }

    private static RegistrationResponse build(Registration r, String qr) {
        return new RegistrationResponse(r.getId(), r.getEvent().getId(), r.getEvent().getTitle(),
                r.getUser().getFullName(), r.getUser().getEmail(),
                r.getStudentId(), r.getOrganization(), r.getStatus(), qr);
    }
}
