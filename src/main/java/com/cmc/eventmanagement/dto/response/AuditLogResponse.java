package com.cmc.eventmanagement.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, Long actorId, String action, String detail, LocalDateTime createdAt) {}
