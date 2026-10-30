package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.CheckInResponse;
import com.cmc.eventmanagement.dto.ScanRequest;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService checkInService;

    @PostMapping("/scan")
    public CheckInResponse scan(@Valid @RequestBody ScanRequest req, @AuthenticationPrincipal AppUserDetails me) {
        return checkInService.scan(req, me);
    }
}
