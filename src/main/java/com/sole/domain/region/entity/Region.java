package com.sole.domain.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "regions",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_region_city_district", columnNames = {"city", "district"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 50)
    private String district;

    public Region(String city, String district) {
        this.city = city;
        this.district = district;
    }
}
