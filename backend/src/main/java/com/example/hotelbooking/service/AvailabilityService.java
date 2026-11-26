package com.example.hotelbooking.service;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.RoomInventory;
import com.example.hotelbooking.dto.DateOption;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AvailabilityService {

    /** How many days before/after the requested check-in we look for alternatives. */
    static final int SEARCH_RADIUS_DAYS = 14;
    static final int MAX_SUGGESTIONS = 3;

    private final RoomInventoryRepository inventoryRepository;
    private final PricingService pricingService;
    private final Clock clock;

    public AvailabilityService(RoomInventoryRepository inventoryRepository,
                               PricingService pricingService,
                               Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.pricingService = pricingService;
        this.clock = clock;
    }

    /**
     * Rooms that can still be booked for the whole stay. A stay is only as available as its
     * fullest night, so we take the minimum across all nights. If any night has no inventory
     * row (too far in the future) the answer is 0.
     */
    @Transactional(readOnly = true)
    public int availableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        int nights = pricingService.nights(checkIn, checkOut);
        List<RoomInventory> rows = inventoryRepository.findRange(hotelId, checkIn, checkOut);
        if (rows.size() < nights) {
            return 0;
        }
        return rows.stream().mapToInt(RoomInventory::availableRooms).min().orElse(0);
    }

    /**
     * SURPRISE FEATURE. Looks at stays of the same length that start up to 14 days earlier or later
     * and returns the (up to 3) closest ones that still have a free room on every night.
     */
    @Transactional(readOnly = true)
    public List<DateOption> findAlternatives(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        int nights = pricingService.nights(checkIn, checkOut);
        LocalDate today = LocalDate.now(clock);

        // One query for the whole window; then everything is decided in memory.
        LocalDate windowStart = checkIn.minusDays(SEARCH_RADIUS_DAYS);
        LocalDate windowEnd = checkOut.plusDays(SEARCH_RADIUS_DAYS);
        Map<LocalDate, Integer> freeByNight = new HashMap<>();
        for (RoomInventory row : inventoryRepository.findRange(hotel.getId(), windowStart, windowEnd)) {
            freeByNight.put(row.getStayDate(), row.availableRooms());
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int shift = -SEARCH_RADIUS_DAYS; shift <= SEARCH_RADIUS_DAYS; shift++) {
            if (shift == 0) {
                continue;
            }
            LocalDate start = checkIn.plusDays(shift);
            if (start.isBefore(today)) {
                continue;
            }
            LocalDate end = start.plusDays(nights);
            int rooms = minFreeRooms(freeByNight, start, end);
            if (rooms > 0) {
                BigDecimal total = pricingService.total(hotel.getPricePerNight(), nights);
                candidates.add(new Candidate(shift, new DateOption(start, end, nights, rooms, total)));
            }
        }

        // Closest to the requested dates first; on a tie, the earlier stay first.
        candidates.sort(Comparator
                .comparingInt((Candidate c) -> Math.abs(c.shift()))
                .thenComparingInt(Candidate::shift));

        return candidates.stream()
                .limit(MAX_SUGGESTIONS)
                .map(Candidate::option)
                .toList();
    }

    private int minFreeRooms(Map<LocalDate, Integer> freeByNight, LocalDate start, LocalDate end) {
        int min = Integer.MAX_VALUE;
        for (LocalDate night = start; night.isBefore(end); night = night.plusDays(1)) {
            min = Math.min(min, freeByNight.getOrDefault(night, 0));
            if (min == 0) {
                return 0;
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private record Candidate(int shift, DateOption option) {
    }
}
