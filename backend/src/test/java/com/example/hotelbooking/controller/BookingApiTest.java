package com.example.hotelbooking.controller;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import com.example.hotelbooking.service.InventoryService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomInventoryRepository inventoryRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private InventoryService inventoryService;

    private final LocalDate today = LocalDate.now();
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        inventoryRepository.deleteAll();
        hotelRepository.deleteAll();
        hotel = hotelRepository.save(new Hotel("API Test Hotel", "Testville", "Used by API tests.",
                "Free Wi-Fi", new BigDecimal("2000.00"), 1));
        inventoryService.ensureInventory(hotel, today, today.plusDays(60));
    }

    private String bookingJson(String email, LocalDate checkIn, LocalDate checkOut) {
        return """
                {"hotelId": %d, "guestName": "Asha Rao", "guestEmail": "%s", "checkIn": "%s", "checkOut": "%s"}
                """.formatted(hotel.getId(), email, checkIn, checkOut);
    }

    @Test
    void createBooking_returns201WithReferenceAndTheBackendTotal() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("asha@example.com", today.plusDays(10), today.plusDays(13))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference", startsWith("HB-")))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.totalAmount").value(6000.0));
    }

    @Test
    void invalidEmail_returns400WithAFieldError() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("not-an-email", today.plusDays(10), today.plusDays(13))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("guestEmail"));
    }

    @Test
    void pastCheckIn_returns400WithAClearMessage() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("asha@example.com", today.minusDays(2), today.plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Check-in date cannot be in the past."));
    }

    @Test
    void lastRoomAlreadyTaken_returns409() throws Exception {
        String body = bookingJson("asha@example.com", today.plusDays(10), today.plusDays(12));
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_AVAILABILITY"));
    }

    @Test
    void unknownReference_returns404() throws Exception {
        mockMvc.perform(get("/api/bookings/HB-UNKNOWN1").param("email", "asha@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void malformedDateParameter_returns400() throws Exception {
        mockMvc.perform(get("/api/hotels").param("checkIn", "tomorrow").param("checkOut", "later"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void bookLookupCancel_fullFlowReleasesAvailability() throws Exception {
        LocalDate in = today.plusDays(10);
        LocalDate out = today.plusDays(12);

        MvcResult created = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("asha@example.com", in, out)))
                .andExpect(status().isCreated())
                .andReturn();
        String reference = JsonPath.read(created.getResponse().getContentAsString(), "$.reference");

        mockMvc.perform(get("/api/hotels/{id}", hotel.getId())
                        .param("checkIn", in.toString()).param("checkOut", out.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stay.availableRooms").value(0));

        mockMvc.perform(get("/api/bookings/{reference}", reference).param("email", "asha@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/bookings/{reference}/cancel", reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"asha@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/hotels/{id}", hotel.getId())
                        .param("checkIn", in.toString()).param("checkOut", out.toString()))
                .andExpect(jsonPath("$.stay.availableRooms").value(1));
    }
}
