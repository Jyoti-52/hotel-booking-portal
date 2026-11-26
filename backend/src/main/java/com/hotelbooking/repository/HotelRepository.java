package com.hotelbooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.domain.Hotel;

import jakarta.persistence.LockModeType;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOrderByNameAsc();

    List<Hotel> findByLocationContainingIgnoreCaseOrderByNameAsc(String location);

    /**
     * Loads the hotel with SELECT ... FOR UPDATE. Every booking and cancellation for a hotel
     * takes this lock first, which serialises them and prevents two guests from taking the last room.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.id = :id")
    Optional<Hotel> findByIdForUpdate(@Param("id") Long id);
}
