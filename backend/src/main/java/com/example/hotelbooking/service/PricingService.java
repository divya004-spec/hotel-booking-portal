package com.example.hotelbooking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PricingService {

    /** Number of nights: check-in night up to, but not including, the check-out date. */
    public int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Total amount = price per night x nights, with exactly 2 decimal places. */
    public BigDecimal total(BigDecimal pricePerNight, int nights) {
        return pricePerNight
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
