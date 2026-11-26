-- Hotels: one room category per hotel, so a hotel only needs a price and a room count.
CREATE TABLE hotels (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)  NOT NULL,
    location        VARCHAR(100)  NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    amenities       VARCHAR(500)  NOT NULL,   -- comma separated, e.g. 'Free Wi-Fi,Swimming pool'
    price_per_night DECIMAL(10,2) NOT NULL,
    total_rooms     INT           NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_hotels_location (location),
    CONSTRAINT chk_hotels_price CHECK (price_per_night > 0),
    CONSTRAINT chk_hotels_rooms CHECK (total_rooms > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Room inventory: one row per hotel per night.
-- A guest staying the nights 10, 11 and 12 uses three rows. Availability = total_rooms - booked_rooms.
CREATE TABLE room_inventory (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id     BIGINT NOT NULL,
    stay_date    DATE   NOT NULL,
    total_rooms  INT    NOT NULL,
    booked_rooms INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_hotel_date (hotel_id, stay_date),
    CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    -- Last line of defence against overbooking, even if application code has a bug.
    CONSTRAINT chk_inventory_bounds CHECK (booked_rooms >= 0 AND booked_rooms <= total_rooms)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Bookings: price per night is copied in so old bookings keep their price if a hotel changes its rate.
CREATE TABLE bookings (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    reference       VARCHAR(20)   NOT NULL,
    hotel_id        BIGINT        NOT NULL,
    guest_name      VARCHAR(100)  NOT NULL,
    guest_email     VARCHAR(150)  NOT NULL,
    check_in        DATE          NOT NULL,
    check_out       DATE          NOT NULL,
    nights          INT           NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    total_amount    DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,   -- CONFIRMED or CANCELLED
    created_at      DATETIME(6)   NOT NULL,
    cancelled_at    DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookings_reference (reference),
    INDEX idx_bookings_hotel_dates (hotel_id, check_in, check_out),
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_bookings_dates CHECK (check_out > check_in)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
