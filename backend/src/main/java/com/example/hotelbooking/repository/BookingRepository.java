package com.example.hotelbooking.repository;

import com.example.hotelbooking.domain.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByReference(String reference);

    /** Reference and email must both match (email is compared ignoring case). */
    @Query("""
            select b from Booking b
            where b.reference = :reference
              and lower(b.guestEmail) = lower(:email)
            """)
    Optional<Booking> findByReferenceAndEmail(@Param("reference") String reference,
                                              @Param("email") String email);

    /** Same lookup, but locked so two cancel requests for one booking cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from Booking b
            where b.reference = :reference
              and lower(b.guestEmail) = lower(:email)
            """)
    Optional<Booking> findByReferenceAndEmailForUpdate(@Param("reference") String reference,
                                                       @Param("email") String email);
}
