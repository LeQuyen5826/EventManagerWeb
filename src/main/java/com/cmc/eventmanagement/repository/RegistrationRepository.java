package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.Registration;
import com.cmc.eventmanagement.entity.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("select count(r) from Registration r where r.event.id = :eventId and r.status in :statuses")
    long countByEventAndStatuses(@Param("eventId") Long eventId, @Param("statuses") List<RegistrationStatus> statuses);

    /** BR03: số chỗ đã bị chiếm = CONFIRMED + CHECKED_IN. */
    default long countSeatsTaken(Long eventId) {
        return countByEventAndStatuses(eventId, List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.CHECKED_IN));
    }

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    /** Check-in: khóa dòng để quét trùng đồng thời không check-in hai lần. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Registration r where r.qrToken = :token")
    Optional<Registration> findByQrTokenForUpdate(@Param("token") String token);

    /** FR32: người đầu danh sách chờ. */
    Optional<Registration> findFirstByEventIdAndStatusOrderByCreatedAtAsc(Long eventId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"event", "user"})
    List<Registration> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"event", "user"})
    Page<Registration> findByEventId(Long eventId, Pageable pageable);

    /** Khi hủy sự kiện: hủy hàng loạt các đăng ký còn hiệu lực, QR mất hiệu lực. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Registration r set r.status = :to, r.qrToken = null "
            + "where r.event.id = :eventId and r.status in :statuses")
    int bulkUpdateStatus(@Param("eventId") Long eventId,
                         @Param("statuses") Collection<RegistrationStatus> statuses,
                         @Param("to") RegistrationStatus to);
}
