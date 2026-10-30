package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
