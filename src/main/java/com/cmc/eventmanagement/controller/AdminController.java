package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.request.*;
import com.cmc.eventmanagement.dto.response.*;
import com.cmc.eventmanagement.entity.EventStatus;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Toàn bộ /api/admin/** đã bị chặn ở SecurityFilterChain: hasRole("ADMIN"). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final EventService eventService;
    private final CatalogService catalogService;
    private final ReportService reportService;
    private final AuditLogService auditLogService;

    // ---- người dùng (FR05, FR06)
    @GetMapping("/users")
    public Page<UserResponse> users(@RequestParam(required = false) String keyword,
                                    @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.search(keyword, pageable);
    }

    @PutMapping("/users/{id}/roles")
    public UserResponse setRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest req,
                                 @AuthenticationPrincipal AppUserDetails actor) {
        return userService.setRoles(id, req.roles(), actor.getId());
    }

    @PostMapping("/users/{id}/lock")
    public UserResponse lock(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return userService.setEnabled(id, false, actor.getId());
    }

    @PostMapping("/users/{id}/unlock")
    public UserResponse unlock(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return userService.setEnabled(id, true, actor.getId());
    }

    // ---- duyệt sự kiện (FR12)
    @GetMapping("/events")
    public Page<EventSummaryResponse> eventsByStatus(@RequestParam(defaultValue = "PENDING_APPROVAL") EventStatus status,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return eventService.byStatus(status, pageable);
    }

    @PostMapping("/events/{id}/approve")
    public EventResponse approve(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.approve(id, actor);
    }

    @PostMapping("/events/{id}/reject")
    public EventResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest req,
                                @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.reject(id, req.reason(), actor);
    }

    // ---- danh mục (FR14, FR15)
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(req));
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return catalogService.updateCategory(id, req);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/venues")
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody VenueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createVenue(req));
    }

    @PutMapping("/venues/{id}")
    public VenueResponse updateVenue(@PathVariable Long id, @Valid @RequestBody VenueRequest req) {
        return catalogService.updateVenue(id, req);
    }

    @DeleteMapping("/venues/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
        catalogService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }

    // ---- dashboard + nhật ký (FR54, FR58)
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() { return reportService.dashboard(); }

    @GetMapping("/audit-logs")
    public Page<AuditLogResponse> auditLogs(@PageableDefault(size = 50) Pageable pageable) {
        return auditLogService.list(pageable);
    }
}
