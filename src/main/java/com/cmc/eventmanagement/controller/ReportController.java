package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.response.EventReportResponse;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    /** FR53: ORGANIZER là chủ sự kiện hoặc ADMIN (BR08). */
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventReportResponse report(@PathVariable Long eventId, @AuthenticationPrincipal AppUserDetails actor) {
        return reportService.eventReport(eventId, actor);
    }

    /** FR55: xuất Excel. */
    @GetMapping("/report/export")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<byte[]> export(@PathVariable Long eventId, @AuthenticationPrincipal AppUserDetails actor) {
        byte[] data = reportService.exportRegistrationsExcel(eventId, actor);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations-" + eventId + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
