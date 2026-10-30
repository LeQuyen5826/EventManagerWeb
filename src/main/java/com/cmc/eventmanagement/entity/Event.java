package com.cmc.eventmanagement.entity;

import com.cmc.eventmanagement.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor
public class Event {
    /** BR05: chỉ được hủy đăng ký trước giờ bắt đầu ít nhất số giờ này. */
    public static final long CANCEL_DEADLINE_HOURS = 24;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private EventCategory category;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false) private LocalDateTime startAt;
    @Column(nullable = false) private LocalDateTime endAt;
    @Column(nullable = false) private LocalDateTime registrationOpenAt;
    @Column(nullable = false) private LocalDateTime registrationCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventSession> sessions = new ArrayList<>();

    // Cố ý KHÔNG có List<Registration>: có thể hàng nghìn bản ghi, truy vấn qua RegistrationRepository.
    @ManyToMany
    @JoinTable(name = "event_staff",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> staff = new HashSet<>();

    public boolean isOrganizedBy(Long userId) {
        return organizer != null && organizer.getId().equals(userId);
    }

    /** BR05 */
    public void assertCancellable(LocalDateTime now) {
        if (now.isAfter(startAt.minusHours(CANCEL_DEADLINE_HOURS))) {
            throw new BusinessException("Chỉ được hủy đăng ký trước giờ bắt đầu ít nhất "
                    + CANCEL_DEADLINE_HOURS + " giờ");
        }
    }
}
