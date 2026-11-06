package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** FR30: khóa ghi dòng sự kiện để các request đăng ký đồng thời xếp hàng tuần tự. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    /** FR24, FR25: tìm kiếm, lọc, phân trang. @EntityGraph nạp venue + category trong 1 truy vấn (tránh N+1). */
    @EntityGraph(attributePaths = {"venue", "category"})
    @Query("""
           select e from Event e
           where e.status = :status
             and (:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%')))
             and (:categoryId is null or e.category.id = :categoryId)
             and (:venueId is null or e.venue.id = :venueId)
             and (:startFrom is null or e.startAt >= :startFrom)
             and (:startTo is null or e.startAt <= :startTo)
           """)
    Page<Event> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                       @Param("venueId") Long venueId, @Param("startFrom") LocalDateTime startFrom,
                       @Param("startTo") LocalDateTime startTo, @Param("status") EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"venue", "category"})
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    long countByStatus(EventStatus status);

    @EntityGraph(attributePaths = {"venue", "category"})
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /** Dùng cho scheduler cập nhật trạng thái. */
    List<Event> findByStatusAndStartAtLessThanEqual(EventStatus status, LocalDateTime time);
    List<Event> findByStatusAndEndAtLessThanEqual(EventStatus status, LocalDateTime time);
}
