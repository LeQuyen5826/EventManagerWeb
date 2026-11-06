package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(name = "is_read")
    private boolean read;

    private LocalDateTime createdAt = LocalDateTime.now();

    public static Notification of(User user, String title, String content) {
        Notification n = new Notification();
        n.user = user;
        n.title = title;
        n.content = content;
        return n;
    }

    /** FR32: thông báo người chờ được chuyển lên danh sách chính thức. */
    public static Notification promoted(Registration reg) {
        return of(reg.getUser(), "Bạn đã được xác nhận tham gia",
                "Có chỗ trống, bạn đã được chuyển từ danh sách chờ sang xác nhận cho sự kiện \"" + reg.getEvent().getTitle() + "\".");
    }
}
