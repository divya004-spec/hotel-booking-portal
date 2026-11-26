package com.example.hotelbooking.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    @Test
    void nights_areCountedFromCheckInUpToCheckOut() {
        int nights = pricing.nights(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 23));
        assertThat(nights).isEqualTo(3);
    }

    @Test
    void nights_workAcrossMonthEndInALeapYear() {
        int nights = pricing.nights(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 1));
        assertThat(nights).isEqualTo(2);
    }

    @Test
    void total_isPricePerNightTimesNights() {
        BigDecimal total = pricing.total(new BigDecimal("3499.50"), 3);
        assertThat(total).isEqualByComparingTo("10498.50");
    }

    @Test
    void total_alwaysHasTwoDecimalPlaces() {
        BigDecimal total = pricing.total(new BigDecimal("4800"), 1);
        assertThat(total.scale()).isEqualTo(2);
        assertThat(total).isEqualByComparingTo("4800.00");
    }
}
