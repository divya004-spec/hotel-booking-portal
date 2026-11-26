package com.example.hotelbooking.repository;

import com.example.hotelbooking.domain.RoomInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    /** Inventory rows for the nights startDate (inclusive) to endDate (exclusive), without locking. */
    @Query("""
            select i from RoomInventory i
            where i.hotel.id = :hotelId
              and i.stayDate >= :startDate
              and i.stayDate < :endDate
            order by i.stayDate
            """)
    List<RoomInventory> findRange(@Param("hotelId") Long hotelId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /** Same rows, but locked for writing until the surrounding transaction ends. Always in date order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from RoomInventory i
            where i.hotel.id = :hotelId
              and i.stayDate >= :startDate
              and i.stayDate < :endDate
            order by i.stayDate
            """)
    List<RoomInventory> findRangeForUpdate(@Param("hotelId") Long hotelId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("""
            select i.stayDate from RoomInventory i
            where i.hotel.id = :hotelId
              and i.stayDate >= :startDate
              and i.stayDate < :endDate
            """)
    List<LocalDate> findExistingDates(@Param("hotelId") Long hotelId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}
