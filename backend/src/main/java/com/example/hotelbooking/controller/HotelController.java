package com.example.hotelbooking.controller;

import com.example.hotelbooking.dto.DateOption;
import com.example.hotelbooking.dto.HotelResponse;
import com.example.hotelbooking.service.HotelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /** GET /api/hotels?location=goa&checkIn=2026-12-20&checkOut=2026-12-23 (all parameters optional) */
    @GetMapping
    public List<HotelResponse> search(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.search(location, checkIn, checkOut);
    }

    /** GET /api/hotels/locations: city names for the search box suggestions. */
    @GetMapping("/locations")
    public List<String> locations() {
        return hotelService.locations();
    }

    /** GET /api/hotels/3?checkIn=...&checkOut=...: details, available rooms and total price. */
    @GetMapping("/{id}")
    public HotelResponse get(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.get(id, checkIn, checkOut);
    }

    /** GET /api/hotels/3/alternatives?checkIn=...&checkOut=...: the surprise feature. */
    @GetMapping("/{id}/alternatives")
    public List<DateOption> alternatives(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.alternatives(id, checkIn, checkOut);
    }
}
