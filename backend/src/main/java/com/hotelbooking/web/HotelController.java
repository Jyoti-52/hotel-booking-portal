package com.hotelbooking.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.dto.AlternativeStayResponse;
import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.service.AlternativeDatesService;
import com.hotelbooking.service.HotelService;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final AlternativeDatesService alternativeDatesService;

    public HotelController(HotelService hotelService, AlternativeDatesService alternativeDatesService) {
        this.hotelService = hotelService;
        this.alternativeDatesService = alternativeDatesService;
    }

    /** GET /api/hotels?location=goa&checkIn=2026-12-01&checkOut=2026-12-04 */
    @GetMapping
    public List<HotelResponse> search(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.search(location, checkIn, checkOut);
    }

    /** GET /api/hotels/3?checkIn=...&checkOut=... */
    @GetMapping("/{id}")
    public HotelResponse get(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.getHotel(id, checkIn, checkOut);
    }

    /** Surprise feature: nearby dates with rooms left. */
    @GetMapping("/{id}/alternatives")
    public List<AlternativeStayResponse> alternatives(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return alternativeDatesService.suggest(id, checkIn, checkOut);
    }
}
