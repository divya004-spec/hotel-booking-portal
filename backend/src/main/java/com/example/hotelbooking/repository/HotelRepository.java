package com.example.hotelbooking.repository;

import com.example.hotelbooking.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOrderByNameAsc();

    List<Hotel> findByLocationContainingIgnoreCaseOrderByNameAsc(String location);

    @Query("select distinct h.location from Hotel h order by h.location")
    List<String> findDistinctLocations();
}
