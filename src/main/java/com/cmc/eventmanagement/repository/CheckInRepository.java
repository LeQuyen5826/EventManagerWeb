package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {}
