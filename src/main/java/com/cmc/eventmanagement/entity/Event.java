package com.cmc.eventmanagement.entity;

import com.cmc.eventmanagement.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private EventCategory category;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private LocalDateTime startAt;
    @Column(nullable = false)
    private LocalDateTime endAt;
    @Column(nullable = false)
    private LocalDateTime registrationOpenAt;
    @Column(nullable = false)
    private LocalDateTime registrationCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    private String cancelReason;
    private String rejectReason;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "event_staff",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> staff = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    /** BR05: chỉ cho hủy đăng ký trước giờ bắt đầu X giờ. */
    public void assertCancellable(LocalDateTime now, int beforeHours) {
        if (now.isAfter(startAt.minus(Duration.ofHours(beforeHours)))) {
            throw new BusinessException("Chỉ được hủy đăng ký trước giờ bắt đầu " + beforeHours + " giờ");
        }
    }

    public boolean isOwnedBy(Long userId) {
        return organizer != null && organizer.getId().equals(userId);
    }

    public boolean hasStaff(Long userId) {
        return staff.stream().anyMatch(u -> u.getId().equals(userId));
    }
}
