package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.request.ChangePasswordRequest;
import com.cmc.eventmanagement.dto.request.UpdateProfileRequest;
import com.cmc.eventmanagement.dto.response.EventSummaryResponse;
import com.cmc.eventmanagement.dto.response.NotificationResponse;
import com.cmc.eventmanagement.dto.response.RegistrationResponse;
import com.cmc.eventmanagement.dto.response.UserResponse;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.EventService;
import com.cmc.eventmanagement.service.NotificationService;
import com.cmc.eventmanagement.service.RegistrationService;
import com.cmc.eventmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Mọi API "của tôi": chỉ cần đăng nhập, dữ liệu lấy theo người trong token. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {
    private final UserService userService;
    private final RegistrationService registrationService;
    private final EventService eventService;
    private final NotificationService notificationService;

    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal AppUserDetails actor) {
        return userService.profile(actor.getId());
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest req,
                                      @AuthenticationPrincipal AppUserDetails actor) {
        return userService.updateProfile(actor.getId(), req);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                               @AuthenticationPrincipal AppUserDetails actor) {
        userService.changePassword(actor.getId(), req);
        return ResponseEntity.noContent().build();
    }

    /** FR36: sự kiện/vé của tôi. */
    @GetMapping("/registrations")
    public List<RegistrationResponse> myRegistrations(@AuthenticationPrincipal AppUserDetails actor) {
        return registrationService.myRegistrations(actor.getId());
    }

    /** Sự kiện tôi tổ chức (mọi trạng thái, kể cả Nháp). */
    @GetMapping("/events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public Page<EventSummaryResponse> myEvents(
            @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AppUserDetails actor) {
        return eventService.myEvents(actor, pageable);
    }

    /** FR51: thông báo trong hệ thống. */
    @GetMapping("/notifications")
    public Page<NotificationResponse> notifications(@PageableDefault(size = 20) Pageable pageable,
                                                    @AuthenticationPrincipal AppUserDetails actor) {
        return notificationService.list(actor.getId(), pageable);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails actor) {
        notificationService.markRead(id, actor.getId());
        return ResponseEntity.noContent().build();
    }
}
