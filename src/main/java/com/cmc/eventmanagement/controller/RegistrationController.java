package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.request.RegisterRequest;
import com.cmc.eventmanagement.dto.response.AttendeeResponse;
import com.cmc.eventmanagement.dto.response.RegistrationResponse;
import com.cmc.eventmanagement.entity.RegistrationStatus;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    /** FR28: chỉ ATTENDEE. Luồng chi tiết: Hình 3.3 trong báo cáo. */
    @PostMapping("/api/events/{eventId}/registrations")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<RegistrationResponse> register(@PathVariable Long eventId,
                                                         @Valid @RequestBody(required = false) RegisterRequest req,
                                                         @AuthenticationPrincipal AppUserDetails actor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(eventId, actor.getId(), req));
    }

    /** FR35: danh sách đăng ký của sự kiện (tìm theo tên/email, lọc theo trạng thái). */
    @GetMapping("/api/events/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('ORGANIZER','STAFF','ADMIN')")
    public Page<AttendeeResponse> list(@PathVariable Long eventId,
                                       @RequestParam(required = false) RegistrationStatus status,
                                       @RequestParam(required = false) String keyword,
                                       @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
                                       @AuthenticationPrincipal AppUserDetails actor) {
        return registrationService.listAttendees(eventId, status, keyword, pageable, actor);
    }

    /** FR35: người tổ chức xóa/hủy một đăng ký. */
    @DeleteMapping("/api/events/{eventId}/registrations/{registrationId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> removeByOrganizer(@PathVariable Long eventId, @PathVariable Long registrationId,
                                                  @AuthenticationPrincipal AppUserDetails actor) {
        registrationService.cancelByOrganizer(eventId, registrationId, actor);
        return ResponseEntity.noContent().build();
    }

    /** FR33: người tham gia hủy đăng ký của chính mình. */
    @DeleteMapping("/api/registrations/{id}")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        registrationService.cancel(id, actor.getId());
        return ResponseEntity.noContent().build();
    }

    /** FR38: ảnh QR (PNG) của chính mình. */
    @GetMapping(value = "/api/registrations/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('ATTENDEE')")
    public byte[] qr(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return registrationService.qrPng(id, actor.getId());
    }
}
