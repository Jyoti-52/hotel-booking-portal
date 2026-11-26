package com.hotelbooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByReference(String reference);

    Optional<Booking> findByReferenceAndGuestEmail(String reference, String guestEmail);

    /** Used by cancellation to find which hotel row to lock before touching the booking. */
    @Query("select b.hotel.id from Booking b where b.reference = :reference and b.guestEmail = :email")
    Optional<Long> findHotelIdByReferenceAndEmail(@Param("reference") String reference,
                                                  @Param("email") String email);
}
