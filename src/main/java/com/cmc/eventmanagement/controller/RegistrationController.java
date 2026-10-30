package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.RegisterRequest;
import com.cmc.eventmanagement.dto.RegistrationResponse;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    /** FR28: đăng ký tham gia. */
    @PostMapping("/api/events/{eventId}/registrations")
    public ResponseEntity<RegistrationResponse> register(@PathVariable Long eventId,
                                                         @Valid @RequestBody(required = false) RegisterRequest req,
                                                         @AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(eventId, req, me));
    }

    /** Organizer/Admin xem danh sách đăng ký của sự kiện. */
    @GetMapping("/api/events/{eventId}/registrations")
    public Page<RegistrationResponse> listByEvent(@PathVariable Long eventId,
                                                  @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
                                                  @AuthenticationPrincipal AppUserDetails me) {
        return registrationService.listByEvent(eventId, pageable, me);
    }

    /** FR33: hủy đăng ký -> 204. */
    @DeleteMapping("/api/registrations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        registrationService.cancel(id, me);
        return ResponseEntity.noContent().build();
    }

    /** Ảnh QR (PNG) của vé. */
    @GetMapping(value = "/api/registrations/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        return registrationService.qrImage(id, me);
    }
}
