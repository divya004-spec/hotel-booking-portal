package com.example.hotelbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookingRequest(
        @NotNull(message = "Please choose a hotel.")
        Long hotelId,

        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name must be at most 100 characters.")
        String guestName,

        @NotBlank(message = "Email is required.")
        @Size(max = 150, message = "Email must be at most 150 characters.")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Please enter a valid email address.")
        String guestEmail,

        @NotNull(message = "Check-in date is required.")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required.")
        LocalDate checkOut) {
}
