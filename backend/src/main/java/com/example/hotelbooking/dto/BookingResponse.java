package com.example.hotelbooking.dto;

import com.example.hotelbooking.domain.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        String reference,
        String status,
        Long hotelId,
        String hotelName,
        String hotelLocation,
        String guestName,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt) {

    /** Must be called while the database session is open, because it reads the hotel. */
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getReference(),
                booking.getStatus().name(),
                booking.getHotel().getId(),
                booking.getHotel().getName(),
                booking.getHotel().getLocation(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getNights(),
                booking.getPricePerNight(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                booking.getCancelledAt());
    }
}
