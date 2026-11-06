package com.cmc.eventmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venues")
@Getter @Setter @NoArgsConstructor
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    private String address;

    /** BR02: sức chứa sự kiện không vượt quá giá trị này */
    @Column(nullable = false)
    private int maxCapacity;

    public Venue(String name, String address, int maxCapacity) {
        this.name = name;
        this.address = address;
        this.maxCapacity = maxCapacity;
    }
}
