package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Thông báo trong hệ thống (FR51). */
@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read")
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Thông báo: người trong danh sách chờ đã được xác nhận chỗ. */
    public static Notification promoted(Registration r) {
        Notification n = new Notification();
        n.user = r.getUser();
        n.message = "Bạn đã được xác nhận tham gia sự kiện \"" + r.getEvent().getTitle()
                + "\" từ danh sách chờ. Mã QR của bạn đã sẵn sàng.";
        return n;
    }
}
