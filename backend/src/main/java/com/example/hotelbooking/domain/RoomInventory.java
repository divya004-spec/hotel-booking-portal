package com.example.hotelbooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "room_inventory")
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "total_rooms", nullable = false)
    private int totalRooms;

    @Column(name = "booked_rooms", nullable = false)
    private int bookedRooms;

    protected RoomInventory() {
        // required by JPA
    }

    public RoomInventory(Hotel hotel, LocalDate stayDate, int totalRooms) {
        this.hotel = hotel;
        this.stayDate = stayDate;
        this.totalRooms = totalRooms;
        this.bookedRooms = 0;
    }

    public int availableRooms() {
        return totalRooms - bookedRooms;
    }

    /** Takes one room for this night. Refuses to go past capacity. */
    public void reserve() {
        if (availableRooms() < 1) {
            throw new IllegalStateException("No rooms left on " + stayDate);
        }
        bookedRooms++;
    }

    /** Gives one room back for this night. Never goes below zero. */
    public void release() {
        if (bookedRooms > 0) {
            bookedRooms--;
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getStayDate() {
        return stayDate;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public int getBookedRooms() {
        return bookedRooms;
    }
}
