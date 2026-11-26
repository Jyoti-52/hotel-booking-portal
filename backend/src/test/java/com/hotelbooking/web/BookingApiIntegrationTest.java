package com.hotelbooking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void hotelListIncludesNewDemoMetadataWithoutChangingExistingHotels() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andReturn();

        var hotels = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(hotels.size() >= 28);
        assertEquals(21, StreamSupport.stream(hotels.spliterator(), false)
                .filter(hotel -> hotel.hasNonNull("rating"))
                .count());
        Set<String> destinations = StreamSupport.stream(hotels.spliterator(), false)
                .map(hotel -> hotel.path("location").asText())
                .collect(Collectors.toSet());
        assertTrue(destinations.containsAll(Set.of(
                "Mumbai", "Pune", "Goa", "Jaipur", "Delhi", "Bengaluru",
                "Kochi", "Lonavala", "Udaipur", "Manali", "Hyderabad", "Chennai")));
        mockMvc.perform(get("/api/hotels").param("location", "Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        var newHotel = java.util.stream.StreamSupport.stream(hotels.spliterator(), false)
                .filter(hotel -> hotel.path("name").asText().equals("Harbour House Mumbai"))
                .findFirst()
                .orElseThrow();
        assertEquals(4.6, newHotel.path("rating").asDouble());
        assertEquals(284, newHotel.path("reviewCount").asInt());
        assertNotNull(newHotel.path("imageUrl").textValue());

        var existingHotel = java.util.stream.StreamSupport.stream(hotels.spliterator(), false)
                .filter(hotel -> hotel.path("name").asText().equals("Palm Grove Beach Resort"))
                .findFirst()
                .orElseThrow();
        assertFalse(existingHotel.has("rating"));
        assertFalse(existingHotel.has("imageUrl"));
    }

    @Test
    void createsLooksUpAndCancelsBookingThroughTheApi() throws Exception {
        LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(10);
        String email = "api-" + UUID.randomUUID() + "@example.com";
        String bookingRequest = objectMapper.writeValueAsString(Map.of(
                "hotelId", 1,
                "checkIn", checkIn,
                "checkOut", checkIn.plusDays(2),
                "guestName", "API Test Guest",
                "guestEmail", email));

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.nights").value(2))
                .andReturn();
        String reference = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("reference").asText();
        String lookupRequest = objectMapper.writeValueAsString(Map.of("reference", reference, "email", email));

        mockMvc.perform(post("/api/bookings/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lookupRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.guestEmail").value(email));

        mockMvc.perform(post("/api/bookings/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lookupRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
