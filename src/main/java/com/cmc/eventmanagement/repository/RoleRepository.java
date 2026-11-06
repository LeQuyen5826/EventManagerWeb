package com.cmc.eventmanagement.repository;

import com.cmc.eventmanagement.entity.Role;
import com.cmc.eventmanagement.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
