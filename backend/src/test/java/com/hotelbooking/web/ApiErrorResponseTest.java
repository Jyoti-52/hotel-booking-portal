package com.hotelbooking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Checks that failures come back as the consistent ApiError JSON shape. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorResponseTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void invalidBookingRequestReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":1,\"checkIn\":\"2030-01-01\",\"checkOut\":\"2030-01-03\","
                                + "\"guestName\":\"\",\"guestEmail\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void badDateFormatReturnsInvalidParameter() throws Exception {
        mockMvc.perform(get("/api/hotels").param("checkIn", "tomorrow").param("checkOut", "later"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void checkOutBeforeCheckInReturnsInvalidStay() throws Exception {
        mockMvc.perform(get("/api/hotels").param("checkIn", "2099-01-10").param("checkOut", "2099-01-05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STAY"))
                .andExpect(jsonPath("$.message").value("Check-out date must be after the check-in date."));
    }

    @Test
    void unknownHotelReturns404() throws Exception {
        mockMvc.perform(get("/api/hotels/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void sampleHotelsAreLoadedByMigrations() throws Exception {
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }
}
