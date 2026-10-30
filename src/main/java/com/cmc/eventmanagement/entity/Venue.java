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

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Integer maxCapacity;

    public Venue(String name, String address, Integer maxCapacity) {
        this.name = name; this.address = address; this.maxCapacity = maxCapacity;
    }
}
