package com.hotelbooking.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.domain.Hotel;
import com.hotelbooking.domain.RoomInventory;
import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.exception.ApiException;
import com.hotelbooking.exception.ErrorCode;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;

@Service
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final StayPolicy stayPolicy;

    public HotelService(HotelRepository hotelRepository, RoomInventoryRepository inventoryRepository,
                        StayPolicy stayPolicy) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.stayPolicy = stayPolicy;
    }

    /**
     * Lists hotels, optionally filtered by location. When dates are supplied (both are then required)
     * every hotel also carries the number of rooms available and the total price for the stay.
     */
    public List<HotelResponse> search(String location, LocalDate checkIn, LocalDate checkOut) {
        boolean withDates = checkIn != null || checkOut != null;
        if (withDates) {
            stayPolicy.validate(checkIn, checkOut);
        }

        List<Hotel> hotels = (location == null || location.isBlank())
                ? hotelRepository.findAllByOrderByNameAsc()
                : hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc(location.trim());

        if (!withDates || hotels.isEmpty()) {
            return hotels.stream().map(h -> toResponse(h, null, null, null)).toList();
        }

        List<Long> ids = hotels.stream().map(Hotel::getId).toList();
        Map<Long, Map<LocalDate, Integer>> bookedByHotel = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findForHotels(ids, checkIn, checkOut)) {
            bookedByHotel.computeIfAbsent(row.getHotelId(), k -> new HashMap<>())
                    .put(row.getStayDate(), row.getBookedRooms());
        }

        return hotels.stream()
                .map(h -> toResponse(h, checkIn, checkOut,
                        AvailabilityCalculator.availableRooms(h.getTotalRooms(),
                                bookedByHotel.getOrDefault(h.getId(), Map.of()), checkIn, checkOut)))
                .toList();
    }

    /** One hotel, with availability and price for the stay when dates are supplied. */
    public HotelResponse getHotel(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND, "Hotel not found."));

        if (checkIn == null && checkOut == null) {
            return toResponse(hotel, null, null, null);
        }
        stayPolicy.validate(checkIn, checkOut);

        Map<LocalDate, Integer> booked = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findForHotel(id, checkIn, checkOut)) {
            booked.put(row.getStayDate(), row.getBookedRooms());
        }
        int available = AvailabilityCalculator.availableRooms(hotel.getTotalRooms(), booked, checkIn, checkOut);
        return toResponse(hotel, checkIn, checkOut, available);
    }

    private HotelResponse toResponse(Hotel h, LocalDate checkIn, LocalDate checkOut, Integer available) {
        Integer nights = null;
        java.math.BigDecimal total = null;
        if (checkIn != null) {
            nights = PriceCalculator.nights(checkIn, checkOut);
            total = PriceCalculator.total(h.getPricePerNight(), nights);
        }
        return new HotelResponse(h.getId(), h.getName(), h.getLocation(), h.getDescription(),
                h.getAmenityList(), h.getPricePerNight(), h.getTotalRooms(),
                checkIn, checkOut, nights, total,
                available, available == null ? null : available > 0,
                h.getRating(), h.getReviewCount(), h.getImageUrl());
    }
}
