package com.example.hotelbooking.controller;

import com.example.hotelbooking.dto.BookingRequest;
import com.example.hotelbooking.dto.BookingResponse;
import com.example.hotelbooking.dto.CancelRequest;
import com.example.hotelbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** POST /api/bookings: creates a booking and returns it with its reference. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    /** GET /api/bookings/HB-ABCD2345?email=guest@example.com */
    @GetMapping("/{reference}")
    public BookingResponse get(@PathVariable String reference, @RequestParam String email) {
        return bookingService.get(reference, email);
    }

    /** POST /api/bookings/HB-ABCD2345/cancel with body {"email": "guest@example.com"} */
    @PostMapping("/{reference}/cancel")
    public BookingResponse cancel(@PathVariable String reference, @Valid @RequestBody CancelRequest request) {
        return bookingService.cancel(reference, request.email());
    }
}
