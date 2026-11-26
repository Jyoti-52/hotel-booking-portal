-- Hotel booking portal: schema.
-- Written in portable SQL so the same migration runs on MySQL 8 (production)
-- and on H2 in MySQL mode (automated tests).

CREATE TABLE hotels (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    location        VARCHAR(120)  NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    amenities       VARCHAR(500)  NOT NULL,            -- comma separated, e.g. 'WiFi,Pool,Breakfast'
    price_per_night DECIMAL(10,2) NOT NULL,
    total_rooms     INT           NOT NULL,            -- rooms of the single room category
    CONSTRAINT chk_hotels_price CHECK (price_per_night > 0),
    CONSTRAINT chk_hotels_rooms CHECK (total_rooms > 0)
);

CREATE INDEX idx_hotels_location ON hotels (location);

-- One row per hotel per night that has at least one booking.
-- booked_rooms is the number of rooms already reserved for that night.
CREATE TABLE room_inventory (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id     BIGINT NOT NULL,
    stay_date    DATE   NOT NULL,
    booked_rooms INT    NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT uq_inventory_hotel_date UNIQUE (hotel_id, stay_date),
    CONSTRAINT chk_inventory_booked CHECK (booked_rooms >= 0)
);

CREATE TABLE bookings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference       VARCHAR(20)   NOT NULL,
    hotel_id        BIGINT        NOT NULL,
    guest_name      VARCHAR(100)  NOT NULL,
    guest_email     VARCHAR(254)  NOT NULL,            -- stored lower-case
    check_in        DATE          NOT NULL,
    check_out       DATE          NOT NULL,            -- exclusive: the guest leaves on this date
    nights          INT           NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,            -- price snapshot at booking time
    total_amount    DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,            -- CONFIRMED | CANCELLED
    created_at      DATETIME      NOT NULL,
    cancelled_at    DATETIME      NULL,
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT uq_bookings_reference UNIQUE (reference),
    CONSTRAINT chk_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT chk_bookings_nights CHECK (nights > 0)
);

CREATE INDEX idx_bookings_hotel ON bookings (hotel_id);
CREATE INDEX idx_bookings_lookup ON bookings (reference, guest_email);
