package com.hotelbooking.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingStatus;
import com.hotelbooking.domain.Hotel;
import com.hotelbooking.domain.RoomInventory;
import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.exception.ApiException;
import com.hotelbooking.exception.ErrorCode;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;

@Service
public class BookingService {

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final StayPolicy stayPolicy;
    private final BookingReferenceGenerator referenceGenerator;
    private final Clock clock;

    public BookingService(HotelRepository hotelRepository, RoomInventoryRepository inventoryRepository,
                          BookingRepository bookingRepository, StayPolicy stayPolicy,
                          BookingReferenceGenerator referenceGenerator, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
        this.stayPolicy = stayPolicy;
        this.referenceGenerator = referenceGenerator;
        this.clock = clock;
    }

    /**
     * Books one room. The hotel row is locked (SELECT ... FOR UPDATE) for the whole transaction, so two guests
     * competing for the last room are processed one after the other: the second one sees the updated
     * inventory and is rejected with ROOMS_UNAVAILABLE instead of overbooking.
     *
     * READ_COMMITTED matters on MySQL: its default (REPEATABLE READ) can keep showing a snapshot taken before the lock
     * was granted, which would hide the other guest's booking. With READ_COMMITTED every read sees the latest commit.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse create(CreateBookingRequest request) {
        stayPolicy.validate(request.checkIn(), request.checkOut());

        Hotel hotel = hotelRepository.findByIdForUpdate(request.hotelId())
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND, "Hotel not found."));

        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();
        List<LocalDate> nightsList = checkIn.datesUntil(checkOut).toList();

        Map<LocalDate, RoomInventory> rows = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findForHotel(hotel.getId(), checkIn, checkOut)) {
            rows.put(row.getStayDate(), row);
        }

        // 1) check every night first, so nothing is changed if any night is full
        for (LocalDate night : nightsList) {
            RoomInventory row = rows.get(night);
            int booked = row == null ? 0 : row.getBookedRooms();
            if (booked >= hotel.getTotalRooms()) {
                throw new ApiException(ErrorCode.ROOMS_UNAVAILABLE,
                        "Sorry, " + hotel.getName() + " has no rooms left for the night of " + night
                                + ". Please choose different dates.");
            }
        }

        // 2) reserve one room for every night of the stay
        for (LocalDate night : nightsList) {
            RoomInventory row = rows.computeIfAbsent(night, d -> new RoomInventory(hotel.getId(), d));
            row.reserveOne();
            inventoryRepository.save(row);
        }

        int nights = nightsList.size();
        Booking booking = new Booking(
                uniqueReference(),
                hotel,
                request.guestName().trim(),
                normaliseEmail(request.guestEmail()),
                checkIn,
                checkOut,
                nights,
                hotel.getPricePerNight(),
                PriceCalculator.total(hotel.getPricePerNight(), nights),
                LocalDateTime.now(clock));
        bookingRepository.save(booking);
        return toResponse(booking);
    }

    /** Looks a booking up by reference AND email. A wrong email gives the same answer as an unknown reference. */
    @Transactional(readOnly = true)
    public BookingResponse find(String reference, String email) {
        Booking booking = bookingRepository
                .findByReferenceAndGuestEmail(normaliseReference(reference), normaliseEmail(email))
                .orElseThrow(BookingService::notFound);
        return toResponse(booking);
    }

    /**
     * Cancels a booking and releases one room for every night of the stay.
     * Lock order is always hotel first, same as {@link #create}, so the two operations cannot deadlock.
     * Also READ_COMMITTED, for the same reason as {@link #create}.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse cancel(String reference, String email) {
        String ref = normaliseReference(reference);
        String mail = normaliseEmail(email);

        Long hotelId = bookingRepository.findHotelIdByReferenceAndEmail(ref, mail)
                .orElseThrow(BookingService::notFound);
        hotelRepository.findByIdForUpdate(hotelId)
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND, "Hotel not found."));

        // Loaded only after the lock is held, so the status we check is the latest committed one.
        Booking booking = bookingRepository.findByReferenceAndGuestEmail(ref, mail)
                .orElseThrow(BookingService::notFound);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(ErrorCode.ALREADY_CANCELLED, "This booking has already been cancelled.");
        }
        if (!isCancellable(booking)) {
            throw new ApiException(ErrorCode.CANCELLATION_NOT_ALLOWED,
                    "This stay has already started, so it can no longer be cancelled online.");
        }

        for (RoomInventory row : inventoryRepository.findForHotel(hotelId, booking.getCheckIn(), booking.getCheckOut())) {
            row.releaseOne();
            inventoryRepository.save(row);
        }
        booking.cancel(LocalDateTime.now(clock));
        bookingRepository.save(booking);
        return toResponse(booking);
    }

    private boolean isCancellable(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED && !booking.getCheckIn().isBefore(stayPolicy.today());
    }

    private String uniqueReference() {
        String reference;
        do {
            reference = referenceGenerator.next();
        } while (bookingRepository.existsByReference(reference));
        return reference;
    }

    private static String normaliseEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normaliseReference(String reference) {
        return reference == null ? "" : reference.trim().toUpperCase(Locale.ROOT);
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.BOOKING_NOT_FOUND, "No booking found for that reference and email address.");
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getReference(), b.getStatus(),
                b.getHotel().getId(), b.getHotel().getName(), b.getHotel().getLocation(),
                b.getCheckIn(), b.getCheckOut(), b.getNights(),
                b.getPricePerNight(), b.getTotalAmount(),
                b.getGuestName(), b.getGuestEmail(),
                b.getCreatedAt(), b.getCancelledAt(),
                isCancellable(b));
    }
}
