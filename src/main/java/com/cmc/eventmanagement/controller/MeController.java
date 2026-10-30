package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.RegistrationResponse;
import com.cmc.eventmanagement.dto.UserResponse;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.AuthService;
import com.cmc.eventmanagement.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {
    private final AuthService authService;
    private final RegistrationService registrationService;

    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal AppUserDetails me) {
        return authService.profile(me.getId());
    }

    @GetMapping("/registrations")
    public List<RegistrationResponse> myRegistrations(@AuthenticationPrincipal AppUserDetails me) {
        return registrationService.myRegistrations(me);
    }
}
