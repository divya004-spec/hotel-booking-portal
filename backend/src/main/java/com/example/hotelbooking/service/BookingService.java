package com.example.hotelbooking.service;

import com.example.hotelbooking.domain.Booking;
import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.RoomInventory;
import com.example.hotelbooking.dto.BookingRequest;
import com.example.hotelbooking.dto.BookingResponse;
import com.example.hotelbooking.exception.BadRequestException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BookingService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final StayValidator stayValidator;
    private final PricingService pricingService;
    private final BookingReferenceGenerator referenceGenerator;
    private final Clock clock;

    public BookingService(HotelRepository hotelRepository,
                          RoomInventoryRepository inventoryRepository,
                          BookingRepository bookingRepository,
                          StayValidator stayValidator,
                          PricingService pricingService,
                          BookingReferenceGenerator referenceGenerator,
                          Clock clock) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
        this.stayValidator = stayValidator;
        this.pricingService = pricingService;
        this.referenceGenerator = referenceGenerator;
        this.clock = clock;
    }

    /**
     * Books one room. Everything below runs in ONE database transaction:
     * the inventory rows for the stay are locked (SELECT ... FOR UPDATE), checked, updated and
     * the booking is saved. If anything fails, everything is rolled back.
     */
    @Transactional
    public BookingResponse create(BookingRequest request) {
        stayValidator.validate(request.checkIn(), request.checkOut());

        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Hotel not found."));

        int nights = pricingService.nights(request.checkIn(), request.checkOut());

        // A second user booking the same nights waits here until we commit, then sees our update.
        List<RoomInventory> rows = inventoryRepository.findRangeForUpdate(
                hotel.getId(), request.checkIn(), request.checkOut());

        if (rows.size() < nights) {
            throw new BadRequestException("Those dates are too far ahead. Bookings are open for the next 12 months only.");
        }
        boolean soldOutOnSomeNight = rows.stream().anyMatch(row -> row.availableRooms() < 1);
        if (soldOutOnSomeNight) {
            throw new ConflictException("NO_AVAILABILITY",
                    "Sorry, no rooms are available for all of the selected nights. Please try different dates.");
        }

        rows.forEach(RoomInventory::reserve);

        BigDecimal total = pricingService.total(hotel.getPricePerNight(), nights);
        Booking booking = new Booking(
                newUniqueReference(),
                hotel,
                request.guestName().trim(),
                request.guestEmail().trim().toLowerCase(Locale.ROOT),
                request.checkIn(),
                request.checkOut(),
                nights,
                hotel.getPricePerNight(),
                total,
                LocalDateTime.now(clock));
        bookingRepository.save(booking);

        return BookingResponse.from(booking);
    }

    /** Both the reference and the email must match; otherwise we answer "not found" for either. */
    @Transactional(readOnly = true)
    public BookingResponse get(String reference, String email) {
        Booking booking = bookingRepository
                .findByReferenceAndEmail(normalizeReference(reference), email.trim())
                .orElseThrow(BookingService::bookingNotFound);
        return BookingResponse.from(booking);
    }

    /** Cancels a booking and gives the room back on every night of the stay. */
    @Transactional
    public BookingResponse cancel(String reference, String email) {
        Booking booking = bookingRepository
                .findByReferenceAndEmailForUpdate(normalizeReference(reference), email.trim())
                .orElseThrow(BookingService::bookingNotFound);

        if (booking.isCancelled()) {
            throw new ConflictException("ALREADY_CANCELLED", "This booking has already been cancelled.");
        }
        if (booking.getCheckIn().isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("This stay has already started, so it can no longer be cancelled online.");
        }

        List<RoomInventory> rows = inventoryRepository.findRangeForUpdate(
                booking.getHotel().getId(), booking.getCheckIn(), booking.getCheckOut());
        rows.forEach(RoomInventory::release);

        booking.cancel(LocalDateTime.now(clock));
        return BookingResponse.from(booking);
    }

    private String newUniqueReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String candidate = referenceGenerator.next();
            if (!bookingRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique booking reference.");
    }

    private static String normalizeReference(String reference) {
        return reference == null ? "" : reference.trim().toUpperCase(Locale.ROOT);
    }

    private static NotFoundException bookingNotFound() {
        return new NotFoundException("We could not find a booking with that reference and email address.");
    }
}
