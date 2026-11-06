package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u where :keyword is null or lower(u.email) like lower(concat('%', :keyword, '%')) " +
           "or lower(u.fullName) like lower(concat('%', :keyword, '%'))")
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
