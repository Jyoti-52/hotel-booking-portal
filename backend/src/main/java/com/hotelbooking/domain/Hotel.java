package com.hotelbooking.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String location;

    @Column(nullable = false, length = 1000)
    private String description;

    /** Comma separated list, kept as one column because amenities are only ever displayed. */
    @Column(nullable = false, length = 500)
    private String amenities;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "total_rooms", nullable = false)
    private int totalRooms;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    protected Hotel() {
        // required by JPA
    }

    public Hotel(String name, String location, String description, String amenities,
                 BigDecimal pricePerNight, int totalRooms) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.amenities = amenities;
        this.pricePerNight = pricePerNight;
        this.totalRooms = totalRooms;
    }

    public List<String> getAmenityList() {
        if (amenities == null || amenities.isBlank()) {
            return List.of();
        }
        return Arrays.stream(amenities.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getAmenities() { return amenities; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public int getTotalRooms() { return totalRooms; }
    public BigDecimal getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public String getImageUrl() { return imageUrl; }
}
