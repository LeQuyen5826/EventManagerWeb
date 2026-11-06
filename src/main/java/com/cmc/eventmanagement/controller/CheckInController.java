package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.request.ScanRequest;
import com.cmc.eventmanagement.dto.response.CheckInResponse;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService checkInService;

    /** FR40: quét QR. STAFF được phân công hoặc ORGANIZER là chủ sự kiện. */
    @PostMapping("/api/checkin/scan")
    @PreAuthorize("hasAnyRole('STAFF','ORGANIZER')")
    public CheckInResponse scan(@Valid @RequestBody ScanRequest req, @AuthenticationPrincipal AppUserDetails actor) {
        return checkInService.scan(req.qrToken(), actor);
    }

    /** FR42: check-in thủ công theo mã đăng ký (tìm bằng GET /api/events/{id}/registrations?keyword=). */
    @PostMapping("/api/checkin/manual/{registrationId}")
    @PreAuthorize("hasAnyRole('STAFF','ORGANIZER')")
    public CheckInResponse manual(@PathVariable Long registrationId, @AuthenticationPrincipal AppUserDetails actor) {
        return checkInService.manual(registrationId, actor);
    }

    /** FR43: hoàn tác. */
    @PostMapping("/api/checkin/undo/{registrationId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> undo(@PathVariable Long registrationId, @AuthenticationPrincipal AppUserDetails actor) {
        checkInService.undo(registrationId, actor);
        return ResponseEntity.noContent().build();
    }

    /** FR44: số liệu check-in gần thời gian thực (frontend gọi định kỳ). */
    @GetMapping("/api/events/{eventId}/checkin-stats")
    @PreAuthorize("hasAnyRole('STAFF','ORGANIZER','ADMIN')")
    public Map<String, Long> stats(@PathVariable Long eventId, @AuthenticationPrincipal AppUserDetails actor) {
        return checkInService.stats(eventId, actor);
    }
}
