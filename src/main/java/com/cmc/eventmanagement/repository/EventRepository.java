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

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** FR30: khóa ghi dòng sự kiện để hai người đăng ký/hủy cùng lúc không vượt sức chứa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    /** FR24, FR25: tìm kiếm + lọc + phân trang. @EntityGraph nạp venue, category trong một truy vấn (tránh N+1). */
    @EntityGraph(attributePaths = {"venue", "category"})
    @Query("""
           select e from Event e
           where e.status = :status
             and lower(e.title) like lower(concat('%', :keyword, '%'))
             and (:categoryId is null or e.category.id = :categoryId)
           """)
    Page<Event> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                       @Param("status") EventStatus status, Pageable pageable);

    /** BR07: người dùng là organizer hoặc nhân viên được phân công của sự kiện? (>0 là đúng) */
    @Query("""
           select count(e) from Event e left join e.staff s
           where e.id = :eventId and (e.organizer.id = :userId or s.id = :userId)
           """)
    long countOrganizerOrStaff(@Param("eventId") Long eventId, @Param("userId") Long userId);
}
