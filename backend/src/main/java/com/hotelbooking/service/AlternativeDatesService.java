package com.hotelbooking.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.domain.Hotel;
import com.hotelbooking.domain.RoomInventory;
import com.hotelbooking.dto.AlternativeStayResponse;
import com.hotelbooking.exception.ApiException;
import com.hotelbooking.exception.ErrorCode;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;

/**
 * Surprise feature: "Smart date suggestions".
 *
 * When the requested dates are sold out, the guest would normally have to guess new dates by trial and error.
 * This service looks at the nearest shifted windows (same number of nights, up to {@link #MAX_SHIFT_DAYS}
 * days earlier or later) and returns the closest ones that still have a room.
 */
@Service
@Transactional(readOnly = true)
public class AlternativeDatesService {

    static final int MAX_SHIFT_DAYS = 14;
    static final int MAX_SUGGESTIONS = 3;

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final StayPolicy stayPolicy;

    public AlternativeDatesService(HotelRepository hotelRepository, RoomInventoryRepository inventoryRepository,
                                   StayPolicy stayPolicy) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.stayPolicy = stayPolicy;
    }

    public List<AlternativeStayResponse> suggest(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        stayPolicy.validate(checkIn, checkOut);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND, "Hotel not found."));

        int nights = PriceCalculator.nights(checkIn, checkOut);

        // One query for the whole search window instead of one per candidate.
        Map<LocalDate, Integer> booked = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findForHotel(hotelId,
                checkIn.minusDays(MAX_SHIFT_DAYS), checkOut.plusDays(MAX_SHIFT_DAYS + 1L))) {
            booked.put(row.getStayDate(), row.getBookedRooms());
        }

        List<AlternativeStayResponse> result = new ArrayList<>();
        LocalDate today = stayPolicy.today();

        // Try +1, -1, +2, -2 ... so the closest dates come first.
        for (int shift = 1; shift <= MAX_SHIFT_DAYS && result.size() < MAX_SUGGESTIONS; shift++) {
            for (int direction : new int[] {1, -1}) {
                if (result.size() >= MAX_SUGGESTIONS) {
                    break;
                }
                int offset = shift * direction;
                LocalDate candidateIn = checkIn.plusDays(offset);
                LocalDate candidateOut = checkOut.plusDays(offset);
                if (candidateIn.isBefore(today)) {
                    continue;
                }
                int available = AvailabilityCalculator.availableRooms(
                        hotel.getTotalRooms(), booked, candidateIn, candidateOut);
                if (available > 0) {
                    result.add(new AlternativeStayResponse(candidateIn, candidateOut, nights, offset, available,
                            PriceCalculator.total(hotel.getPricePerNight(), nights)));
                }
            }
        }
        return result;
    }
}
