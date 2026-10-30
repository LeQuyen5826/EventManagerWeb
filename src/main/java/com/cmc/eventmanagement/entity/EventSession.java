package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Một phiên (session) trong chương trình của sự kiện. Lưu/xóa cùng Event (cascade). */
@Entity
@Table(name = "event_sessions")
@Getter @Setter @NoArgsConstructor
public class EventSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false, length = 200)
    private String title;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
