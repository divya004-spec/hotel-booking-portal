package com.example.hotelbooking.service;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.dto.DateOption;
import com.example.hotelbooking.dto.HotelResponse;
import com.example.hotelbooking.dto.StayQuote;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;
    private final StayValidator stayValidator;
    private final AvailabilityService availabilityService;
    private final PricingService pricingService;

    public HotelService(HotelRepository hotelRepository,
                        StayValidator stayValidator,
                        AvailabilityService availabilityService,
                        PricingService pricingService) {
        this.hotelRepository = hotelRepository;
        this.stayValidator = stayValidator;
        this.availabilityService = availabilityService;
        this.pricingService = pricingService;
    }

    /** Lists hotels, optionally filtered by location. With dates, each hotel also carries a quote. */
    @Transactional(readOnly = true)
    public List<HotelResponse> search(String location, LocalDate checkIn, LocalDate checkOut) {
        boolean withDates = checkIn != null || checkOut != null;
        if (withDates) {
            stayValidator.validate(checkIn, checkOut);
        }

        List<Hotel> hotels = (location == null || location.isBlank())
                ? hotelRepository.findAllByOrderByNameAsc()
                : hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc(location.trim());

        return hotels.stream()
                .map(hotel -> HotelResponse.from(hotel, withDates ? quote(hotel, checkIn, checkOut) : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelResponse get(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = findHotel(id);
        boolean withDates = checkIn != null || checkOut != null;
        if (withDates) {
            stayValidator.validate(checkIn, checkOut);
        }
        return HotelResponse.from(hotel, withDates ? quote(hotel, checkIn, checkOut) : null);
    }

    @Transactional(readOnly = true)
    public List<String> locations() {
        return hotelRepository.findDistinctLocations();
    }

    /** Surprise feature: nearby dates that are free. */
    @Transactional(readOnly = true)
    public List<DateOption> alternatives(Long id, LocalDate checkIn, LocalDate checkOut) {
        stayValidator.validate(checkIn, checkOut);
        Hotel hotel = findHotel(id);
        return availabilityService.findAlternatives(hotel, checkIn, checkOut);
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel not found."));
    }

    private StayQuote quote(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        int nights = pricingService.nights(checkIn, checkOut);
        int rooms = availabilityService.availableRooms(hotel.getId(), checkIn, checkOut);
        return new StayQuote(checkIn, checkOut, nights, rooms, rooms > 0,
                pricingService.total(hotel.getPricePerNight(), nights));
    }
}
