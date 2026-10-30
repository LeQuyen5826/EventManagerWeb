package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tên vai trò lưu không có tiền tố: ADMIN, ORGANIZER, ATTENDEE, STAFF. */
@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    public Role(String name) { this.name = name; }
}
