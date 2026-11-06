package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    Optional<CheckIn> findByRegistrationId(Long registrationId);

    @Query("select c.checkedInAt from CheckIn c where c.registration.event.id = :eventId")
    List<LocalDateTime> findCheckInTimesByEvent(@Param("eventId") Long eventId);
}
