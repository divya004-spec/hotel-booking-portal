package com.example.hotelbooking.service;

import com.example.hotelbooking.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class StayValidator {

    public static final int MAX_NIGHTS = 30;

    private final Clock clock;

    public StayValidator(Clock clock) {
        this.clock = clock;
    }

    public void validate(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BadRequestException("Please choose both a check-in and a check-out date.");
        }
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("Check-in date cannot be in the past.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out date must be after the check-in date.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new BadRequestException("You can book at most " + MAX_NIGHTS + " nights at a time.");
        }
    }
}
