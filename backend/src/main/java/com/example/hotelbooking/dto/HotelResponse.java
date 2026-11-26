package com.example.hotelbooking.dto;

import com.example.hotelbooking.domain.Hotel;

import java.math.BigDecimal;
import java.util.List;

/** A hotel, plus a quote for the chosen dates when dates were supplied (otherwise stay is null). */
public record HotelResponse(
        Long id,
        String name,
        String location,
        String description,
        List<String> amenities,
        BigDecimal pricePerNight,
        StayQuote stay) {

    public static HotelResponse from(Hotel hotel, StayQuote stay) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                hotel.getAmenityList(),
                hotel.getPricePerNight(),
                stay);
    }
}
