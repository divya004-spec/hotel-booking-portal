package com.example.hotelbooking.service;

import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.RoomInventory;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryService {

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final Clock clock;
    private final int daysAhead;

    public InventoryService(HotelRepository hotelRepository,
                            RoomInventoryRepository inventoryRepository,
                            Clock clock,
                            @Value("${app.inventory.days-ahead:365}") int daysAhead) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.clock = clock;
        this.daysAhead = daysAhead;
    }

    /** Makes sure every hotel has inventory from today until today + daysAhead. */
    @Transactional
    public int ensureInventoryForAllHotels() {
        LocalDate today = LocalDate.now(clock);
        int created = 0;
        for (Hotel hotel : hotelRepository.findAll()) {
            created += ensureInventory(hotel, today, today.plusDays(daysAhead));
        }
        return created;
    }

    /** Creates the missing nights from startDate (inclusive) to endDate (exclusive). Returns how many were added. */
    @Transactional
    public int ensureInventory(Hotel hotel, LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> existing = new HashSet<>(
                inventoryRepository.findExistingDates(hotel.getId(), startDate, endDate));

        List<RoomInventory> missing = new ArrayList<>();
        for (LocalDate night = startDate; night.isBefore(endDate); night = night.plusDays(1)) {
            if (!existing.contains(night)) {
                missing.add(new RoomInventory(hotel, night, hotel.getTotalRooms()));
            }
        }
        inventoryRepository.saveAll(missing);
        return missing.size();
    }
}
