package com.hotelbooking.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Pure availability rule.
 *
 * A stay occupies every night from check-in up to, but not including, check-out.
 * The rooms available for a stay are those free on its tightest night, i.e.
 * totalRooms minus the highest number of rooms already booked on any of its nights.
 *
 * Two stays overlap only if they share at least one night, so a guest checking out
 * on the 10th does not block another guest checking in on the 10th.
 */
public final class AvailabilityCalculator {

    private AvailabilityCalculator() {
    }

    public static int availableRooms(int totalRooms, Map<LocalDate, Integer> bookedByNight,
                                     LocalDate checkIn, LocalDate checkOut) {
        int maxBooked = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            maxBooked = Math.max(maxBooked, bookedByNight.getOrDefault(night, 0));
        }
        return Math.max(0, totalRooms - maxBooked);
    }
}
