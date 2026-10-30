package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {}
