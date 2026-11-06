package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.request.*;
import com.cmc.eventmanagement.dto.response.EventResponse;
import com.cmc.eventmanagement.dto.response.EventSummaryResponse;
import com.cmc.eventmanagement.dto.response.SessionResponse;
import com.cmc.eventmanagement.entity.EventStatus;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    // ---------- công khai (Khách cũng xem được) ----------

    /** FR23-FR25: ?keyword=&categoryId=&venueId=&from=&to=&status=&page=0&size=10&sort=startAt,asc */
    @GetMapping
    public Page<EventSummaryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return eventService.search(keyword, categoryId, venueId, from, to, status, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.getById(id, actor);
    }

    @GetMapping("/{id}/sessions")
    public List<SessionResponse> sessions(@PathVariable Long id) {
        return eventService.listSessions(id);
    }

    // ---------- Người tổ chức / Admin ----------

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventCreateRequest req,
                                                @AuthenticationPrincipal AppUserDetails actor) {
        EventResponse res = eventService.create(req, actor);
        return ResponseEntity.created(URI.create("/api/events/" + res.id())).body(res);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventCreateRequest req,
                                @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.update(id, req, actor);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse publish(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.publish(id, actor);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest req,
                                @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.cancel(id, req.reason(), actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        eventService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    // ---------- lịch trình ----------

    @PostMapping("/{id}/sessions")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<SessionResponse> addSession(@PathVariable Long id, @Valid @RequestBody SessionRequest req,
                                                      @AuthenticationPrincipal AppUserDetails actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.addSession(id, req, actor));
    }

    @PutMapping("/{id}/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public SessionResponse updateSession(@PathVariable Long id, @PathVariable Long sessionId,
                                         @Valid @RequestBody SessionRequest req,
                                         @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.updateSession(id, sessionId, req, actor);
    }

    @DeleteMapping("/{id}/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id, @PathVariable Long sessionId,
                                              @AuthenticationPrincipal AppUserDetails actor) {
        eventService.deleteSession(id, sessionId, actor);
        return ResponseEntity.noContent().build();
    }

    // ---------- phân công nhân viên (FR16) ----------

    @PostMapping("/{id}/staff")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> assignStaff(@PathVariable Long id, @Valid @RequestBody AssignStaffRequest req,
                                            @AuthenticationPrincipal AppUserDetails actor) {
        eventService.assignStaff(id, req.userId(), actor);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/staff/{userId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> removeStaff(@PathVariable Long id, @PathVariable Long userId,
                                            @AuthenticationPrincipal AppUserDetails actor) {
        eventService.removeStaff(id, userId, actor);
        return ResponseEntity.noContent().build();
    }
}
