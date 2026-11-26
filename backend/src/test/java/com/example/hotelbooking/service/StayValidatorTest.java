package com.example.hotelbooking.service;

import com.example.hotelbooking.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StayValidatorTest {

    // Today is fixed to 2026-11-24 (India time).
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T06:00:00Z"), ZoneId.of("Asia/Kolkata"));
    private final StayValidator validator = new StayValidator(clock);

    private static LocalDate date(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    @Test
    void acceptsANormalFutureStay() {
        assertThatCode(() -> validator.validate(date(12, 20), date(12, 23))).doesNotThrowAnyException();
    }

    @Test
    void acceptsCheckInToday() {
        assertThatCode(() -> validator.validate(date(11, 24), date(11, 25))).doesNotThrowAnyException();
    }

    @Test
    void rejectsCheckInInThePast() {
        assertThatThrownBy(() -> validator.validate(date(11, 23), date(11, 25)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-in date cannot be in the past.");
    }

    @Test
    void rejectsCheckOutOnTheSameDayAsCheckIn() {
        assertThatThrownBy(() -> validator.validate(date(12, 20), date(12, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-out date must be after the check-in date.");
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThatThrownBy(() -> validator.validate(date(12, 23), date(12, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-out date must be after the check-in date.");
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> validator.validate(null, date(12, 20)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate(date(12, 20), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void allowsExactlyThirtyNightsButNotThirtyOne() {
        assertThat(StayValidator.MAX_NIGHTS).isEqualTo(30);
        assertThatCode(() -> validator.validate(date(12, 1), date(12, 31))).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(date(12, 1), LocalDate.of(2027, 1, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at most 30 nights");
    }
}
