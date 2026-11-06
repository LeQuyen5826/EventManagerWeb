package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.response.AuditLogResponse;
import com.cmc.eventmanagement.entity.AuditLog;
import com.cmc.eventmanagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repo;

    /** Chạy trong transaction riêng để nhật ký không bị mất khi nghiệp vụ chính rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long actorId, String action, String detail) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setAction(action);
        a.setDetail(detail);
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Pageable pageable) {
        return repo.findAllByOrderByCreatedAtDesc(pageable)
                .map(a -> new AuditLogResponse(a.getId(), a.getActorId(), a.getAction(), a.getDetail(), a.getCreatedAt()));
    }
}
