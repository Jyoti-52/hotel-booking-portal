package com.hotelbooking.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Number of rooms already reserved at one hotel for one night.
 * Availability for a night = hotel.totalRooms - bookedRooms.
 */
@Entity
@Table(name = "room_inventory")
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "booked_rooms", nullable = false)
    private int bookedRooms;

    protected RoomInventory() {
        // required by JPA
    }

    public RoomInventory(Long hotelId, LocalDate stayDate) {
        this.hotelId = hotelId;
        this.stayDate = stayDate;
        this.bookedRooms = 0;
    }

    public void reserveOne() {
        bookedRooms++;
    }

    public void releaseOne() {
        if (bookedRooms > 0) {
            bookedRooms--;
        }
    }

    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public LocalDate getStayDate() { return stayDate; }
    public int getBookedRooms() { return bookedRooms; }
}
