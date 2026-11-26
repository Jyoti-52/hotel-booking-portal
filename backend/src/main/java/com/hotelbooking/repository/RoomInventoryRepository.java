package com.hotelbooking.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.domain.RoomInventory;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    /** Inventory rows for nights in the half-open range [from, to). */
    @Query("select i from RoomInventory i where i.hotelId = :hotelId and i.stayDate >= :from and i.stayDate < :to")
    List<RoomInventory> findForHotel(@Param("hotelId") Long hotelId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    @Query("select i from RoomInventory i where i.hotelId in :hotelIds and i.stayDate >= :from and i.stayDate < :to")
    List<RoomInventory> findForHotels(@Param("hotelIds") Collection<Long> hotelIds,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);
}
