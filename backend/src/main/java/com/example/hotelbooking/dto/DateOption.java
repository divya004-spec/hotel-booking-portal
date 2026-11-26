package com.example.hotelbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An alternative stay with the same number of nights that is free. */
public record DateOption(
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        int availableRooms,
        BigDecimal totalAmount) {
}
