package com.cmc.eventmanagement.entity;

import com.cmc.eventmanagement.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))   // BR04
@Getter @Setter @NoArgsConstructor
public class Registration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    /** Token ngẫu nhiên, không chứa thông tin cá nhân (FR37). Null khi đang ở danh sách chờ. */
    @Column(unique = true, length = 64)
    private String qrToken;

    private String studentId;
    private String organization;

    private LocalDateTime createdAt = LocalDateTime.now();

    public void confirm(String newQrToken) {
        this.status = RegistrationStatus.CONFIRMED;
        this.qrToken = newQrToken;
    }

    /** FR33, FR39: hủy đăng ký và vô hiệu hóa QR. */
    public void cancel() {
        if (status == RegistrationStatus.CHECKED_IN) {
            throw new BusinessException("Không thể hủy đăng ký đã check-in");
        }
        this.status = RegistrationStatus.CANCELLED;
    }

    public void markCheckedIn() { this.status = RegistrationStatus.CHECKED_IN; }
    public void undoCheckIn() { this.status = RegistrationStatus.CONFIRMED; }
}
