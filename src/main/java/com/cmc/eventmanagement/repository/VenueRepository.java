package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {}
