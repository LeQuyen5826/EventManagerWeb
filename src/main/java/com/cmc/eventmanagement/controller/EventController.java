package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    /** GET /api/events?keyword=&categoryId=&page=0&size=10&sort=startAt,asc */
    @GetMapping
    public Page<EventSummaryResponse> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long categoryId,
                                           @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return eventService.search(keyword, categoryId, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse detail(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        return eventService.getById(id, me);
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventCreateRequest req,
                                                @AuthenticationPrincipal AppUserDetails me) {
        EventResponse created = eventService.create(req, me);
        return ResponseEntity.created(URI.create("/api/events/" + created.id())).body(created);
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        return eventService.publish(id, me);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, String> cancel(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) CancelEventRequest req,
                                      @AuthenticationPrincipal AppUserDetails me) {
        eventService.cancel(id, req == null ? null : req.reason(), me);
        return Map.of("message", "Đã hủy sự kiện");
    }

    @PostMapping("/{id}/staff")
    public Map<String, String> assignStaff(@PathVariable Long id, @Valid @RequestBody AssignStaffRequest req,
                                           @AuthenticationPrincipal AppUserDetails me) {
        eventService.assignStaff(id, req.userId(), me);
        return Map.of("message", "Đã phân công nhân viên");
    }

    @GetMapping("/{id}/report")
    public EventReportResponse report(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        return eventService.report(id, me);
    }
}
