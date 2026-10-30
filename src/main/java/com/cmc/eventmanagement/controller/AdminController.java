package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.UpdateRolesRequest;
import com.cmc.eventmanagement.dto.UserResponse;
import com.cmc.eventmanagement.security.AppUserDetails;
import com.cmc.eventmanagement.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserAdminService userAdminService;

    @GetMapping("/users")
    public Page<UserResponse> users(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return userAdminService.list(pageable);
    }

    @PutMapping("/users/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest req,
                                    @AuthenticationPrincipal AppUserDetails me) {
        return userAdminService.updateRoles(id, req.roles(), me.getId());
    }
}
