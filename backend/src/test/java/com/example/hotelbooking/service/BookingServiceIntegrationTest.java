package com.example.hotelbooking.service;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.dto.BookingRequest;
import com.example.hotelbooking.dto.BookingResponse;
import com.example.hotelbooking.dto.DateOption;
import com.example.hotelbooking.exception.BadRequestException;
import com.example.hotelbooking.exception.ConflictException;
import com.example.hotelbooking.exception.NotFoundException;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BookingServiceIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private InventoryService inventoryService;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomInventoryRepository inventoryRepository;
    @Autowired private BookingRepository bookingRepository;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void cleanDatabase() {
        bookingRepository.deleteAll();
        inventoryRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    private Hotel hotelWithRooms(int rooms) {
        Hotel hotel = hotelRepository.save(new Hotel("Test Hotel", "Testville", "A hotel for tests.",
                "Free Wi-Fi,Pool", new BigDecimal("2000.00"), rooms));
        inventoryService.ensureInventory(hotel, today, today.plusDays(90));
        return hotel;
    }

    private BookingRequest request(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        return new BookingRequest(hotel.getId(), "Asha Rao", "asha@example.com", checkIn, checkOut);
    }

    @Test
    void total_isCalculatedOnTheBackendFromNightsAndPrice() {
        Hotel hotel = hotelWithRooms(2);

        BookingResponse booking = bookingService.create(request(hotel, today.plusDays(10), today.plusDays(13)));

        assertThat(booking.nights()).isEqualTo(3);
        assertThat(booking.totalAmount()).isEqualByComparingTo("6000.00");
        assertThat(booking.status()).isEqualTo("CONFIRMED");
        assertThat(booking.reference()).startsWith("HB-").hasSize(11);
    }

    @Test
    void booking_isRejectedWhenAllRoomsAreTaken() {
        Hotel hotel = hotelWithRooms(2);
        LocalDate in = today.plusDays(10);
        LocalDate out = today.plusDays(12);
        bookingService.create(request(hotel, in, out));
        bookingService.create(request(hotel, in, out));

        assertThatThrownBy(() -> bookingService.create(request(hotel, in, out)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no rooms are available");
        assertThat(availabilityService.availableRooms(hotel.getId(), in, out)).isZero();
    }

    @Test
    void overlappingStay_isRejectedEvenWhenOnlyOneNightClashes() {
        Hotel hotel = hotelWithRooms(1);
        bookingService.create(request(hotel, today.plusDays(10), today.plusDays(13))); // nights 10, 11, 12

        assertThatThrownBy(() -> bookingService.create(request(hotel, today.plusDays(12), today.plusDays(15))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sameDayTurnover_isAllowed() {
        Hotel hotel = hotelWithRooms(1);
        bookingService.create(request(hotel, today.plusDays(10), today.plusDays(13)));

        // The first guest leaves on day 13, the second arrives on day 13: no clash.
        BookingResponse next = bookingService.create(request(hotel, today.plusDays(13), today.plusDays(15)));

        assertThat(next.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void failedBooking_doesNotReserveAnyNight() {
        Hotel hotel = hotelWithRooms(1);
        bookingService.create(request(hotel, today.plusDays(12), today.plusDays(13))); // night 12 taken

        assertThatThrownBy(() -> bookingService.create(request(hotel, today.plusDays(10), today.plusDays(14))))
                .isInstanceOf(ConflictException.class);

        assertThat(availabilityService.availableRooms(hotel.getId(), today.plusDays(10), today.plusDays(12))).isEqualTo(1);
        assertThat(availabilityService.availableRooms(hotel.getId(), today.plusDays(13), today.plusDays(14))).isEqualTo(1);
    }

    @Test
    void cancelling_releasesTheRoomSoItCanBeBookedAgain() {
        Hotel hotel = hotelWithRooms(1);
        LocalDate in = today.plusDays(10);
        LocalDate out = today.plusDays(12);
        BookingResponse booking = bookingService.create(request(hotel, in, out));
        assertThat(availabilityService.availableRooms(hotel.getId(), in, out)).isZero();

        BookingResponse cancelled = bookingService.cancel(booking.reference(), "ASHA@example.com");

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(availabilityService.availableRooms(hotel.getId(), in, out)).isEqualTo(1);
        assertThat(bookingService.create(request(hotel, in, out)).status()).isEqualTo("CONFIRMED");
    }

    @Test
    void cancelling_twiceIsRejected() {
        Hotel hotel = hotelWithRooms(1);
        BookingResponse booking = bookingService.create(request(hotel, today.plusDays(10), today.plusDays(12)));
        bookingService.cancel(booking.reference(), "asha@example.com");

        assertThatThrownBy(() -> bookingService.cancel(booking.reference(), "asha@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been cancelled");
    }

    @Test
    void lookupAndCancel_needTheMatchingEmail() {
        Hotel hotel = hotelWithRooms(1);
        LocalDate in = today.plusDays(10);
        LocalDate out = today.plusDays(12);
        BookingResponse booking = bookingService.create(request(hotel, in, out));

        assertThatThrownBy(() -> bookingService.get(booking.reference(), "someone.else@example.com"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.cancel(booking.reference(), "someone.else@example.com"))
                .isInstanceOf(NotFoundException.class);

        // The wrong-email attempts changed nothing.
        assertThat(bookingService.get(booking.reference(), "asha@example.com").status()).isEqualTo("CONFIRMED");
        assertThat(availabilityService.availableRooms(hotel.getId(), in, out)).isZero();
    }

    @Test
    void pastCheckIn_isRejected() {
        Hotel hotel = hotelWithRooms(1);

        assertThatThrownBy(() -> bookingService.create(request(hotel, today.minusDays(1), today.plusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-in date cannot be in the past.");
    }

    @Test
    void datesBeyondTheInventoryWindow_areRejected() {
        Hotel hotel = hotelWithRooms(1);

        assertThatThrownBy(() -> bookingService.create(request(hotel, today.plusDays(100), today.plusDays(102))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too far ahead");
    }

    @Test
    void unknownHotel_isNotFound() {
        assertThatThrownBy(() -> bookingService.create(
                new BookingRequest(999_999L, "Asha Rao", "asha@example.com", today.plusDays(10), today.plusDays(12))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void alternatives_suggestTheNearestFreeDatesAndNeverOverlapTheBookedOnes() {
        Hotel hotel = hotelWithRooms(1);
        LocalDate in = today.plusDays(30);
        LocalDate out = today.plusDays(32);
        bookingService.create(request(hotel, in, out)); // nights 30 and 31 are now full

        List<DateOption> options = availabilityService.findAlternatives(hotel, in, out);

        assertThat(options).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(options).allSatisfy(option -> {
            assertThat(option.nights()).isEqualTo(2);
            boolean overlapsBookedNights = option.checkIn().isBefore(out) && option.checkOut().isAfter(in);
            assertThat(overlapsBookedNights).isFalse();
        });
        // Closest first: moving two days earlier is the nearest free stay.
        assertThat(options.get(0).checkIn()).isEqualTo(in.minusDays(2));
    }

    @Test
    void usersRacingForTheLastRooms_neverOversell() throws Exception {
        int capacity = 2;
        int users = 6;
        Hotel hotel = hotelWithRooms(capacity);
        LocalDate in = today.plusDays(10);
        LocalDate out = today.plusDays(12);

        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch ready = new CountDownLatch(users);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < users; i++) {
            final int guestNumber = i;
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await(); // all threads start at the same moment
                try {
                    bookingService.create(new BookingRequest(hotel.getId(), "Guest " + guestNumber,
                            "guest" + guestNumber + "@example.com", in, out));
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(capacity);
        assertThat(bookingRepository.count()).isEqualTo(capacity);
        assertThat(availabilityService.availableRooms(hotel.getId(), in, out)).isZero();
    }
}
