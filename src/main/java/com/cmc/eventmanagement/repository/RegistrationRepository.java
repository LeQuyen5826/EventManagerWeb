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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);          // FR29
    Optional<Registration> findByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventId(Long eventId);

    @Query("select count(r) from Registration r where r.event.id = :eventId and r.status in :statuses")
    long countByEventAndStatuses(@Param("eventId") Long eventId, @Param("statuses") Collection<RegistrationStatus> statuses);

    /** BR03: số chỗ đã giữ = CONFIRMED + CHECKED_IN. */
    default long countSeatsTaken(Long eventId) {
        return countByEventAndStatuses(eventId, List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.CHECKED_IN));
    }

    /** Đếm chỗ đã giữ cho nhiều sự kiện cùng lúc (danh sách sự kiện). Trả về [eventId, count]. */
    @Query("select r.event.id, count(r) from Registration r " +
           "where r.event.id in :eventIds and r.status in :statuses group by r.event.id")
    List<Object[]> countSeatsTakenByEvents(@Param("eventIds") Collection<Long> eventIds,
                                           @Param("statuses") Collection<RegistrationStatus> statuses);

    /** FR32: người đứng đầu danh sách chờ. */
    Optional<Registration> findFirstByEventIdAndStatusOrderByCreatedAtAsc(Long eventId, RegistrationStatus status);

    /** Check-in: khóa dòng đăng ký để tránh quét trùng đồng thời (FR41). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Registration r where r.qrToken = :token")
    Optional<Registration> findByQrTokenForUpdate(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Registration r where r.id = :id")
    Optional<Registration> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"event"})
    List<Registration> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    @Query("select r from Registration r where r.event.id = :eventId " +
           "and (:status is null or r.status = :status) " +
           "and (:keyword is null or lower(r.user.fullName) like lower(concat('%', :keyword, '%')) " +
           "     or lower(r.user.email) like lower(concat('%', :keyword, '%')))")
    Page<Registration> searchByEvent(@Param("eventId") Long eventId, @Param("status") RegistrationStatus status,
                                     @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Registration> findByEventIdAndStatusIn(Long eventId, Collection<RegistrationStatus> statuses);

    @Query("select r.status, count(r) from Registration r where r.event.id = :eventId group by r.status")
    List<Object[]> countByStatus(@Param("eventId") Long eventId);

    @Query("select r.createdAt from Registration r where r.event.id = :eventId and r.status <> :excluded")
    List<LocalDateTime> findCreatedAtByEventExcluding(@Param("eventId") Long eventId,
                                                      @Param("excluded") RegistrationStatus excluded);

    /** BR10: sau khi sự kiện kết thúc, CONFIRMED chưa check-in chuyển sang NO_SHOW. */
    @Modifying(flushAutomatically = true)   // flush thay đổi chưa ghi (ví dụ trạng thái sự kiện) trước khi update hàng loạt
    @Query("update Registration r set r.status = :to where r.event.id = :eventId and r.status = :from")
    int bulkChangeStatus(@Param("eventId") Long eventId, @Param("from") RegistrationStatus from,
                         @Param("to") RegistrationStatus to);
}
