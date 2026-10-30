package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_registration_event_user", columnNames = {"event_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class Registration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "qr_token", unique = true, length = 64)
    private String qrToken;

    @Column(length = 30)
    private String studentId;

    @Column(length = 100)
    private String organization;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    /** Có chỗ: xác nhận và cấp mã QR. */
    public void confirm(String token) {
        this.status = RegistrationStatus.CONFIRMED;
        this.qrToken = token;
    }

    /** Hết chỗ: vào danh sách chờ, chưa có QR. */
    public void waitlist() {
        this.status = RegistrationStatus.WAITLISTED;
        this.qrToken = null;
    }

    /** FR39: hủy đăng ký thì mã QR mất hiệu lực. */
    public void cancel() {
        this.status = RegistrationStatus.CANCELLED;
        this.qrToken = null;
    }

    public void checkIn() { this.status = RegistrationStatus.CHECKED_IN; }
}
