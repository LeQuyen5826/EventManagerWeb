package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR58: nhật ký thao tác. Không có khóa ngoại để giữ lịch sử khi dữ liệu gốc thay đổi. */
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;
    private String action;

    @Column(length = 1000)
    private String detail;

    private LocalDateTime createdAt = LocalDateTime.now();
}
