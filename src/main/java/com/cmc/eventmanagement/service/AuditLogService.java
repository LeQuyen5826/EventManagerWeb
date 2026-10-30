package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.entity.AuditLog;
import com.cmc.eventmanagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Ghi nhật ký thao tác quan trọng (FR58): ai làm gì. */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepo;

    public void log(Long actorId, String action, String detail) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setAction(action);
        a.setDetail(detail);
        auditLogRepo.save(a);
    }
}
