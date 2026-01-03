CREATE TABLE reservation_items
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT       NOT NULL,
    slot_id        BIGINT       NOT NULL,

    price_amount   BIGINT       NOT NULL,
    currency       CHAR(3)      NOT NULL DEFAULT 'KRW',

    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- 동일 예약, 같은 슬롯+리소스 중복 방지
    UNIQUE KEY uk_reservation_items_unique (reservation_id, slot_id),

    KEY idx_reservation_items_reservation (reservation_id),
    KEY idx_reservation_items_slot (slot_id),

    CHECK (price_amount >= 0),

    CONSTRAINT fk_reservation_items_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_reservation_items_slot
        FOREIGN KEY (slot_id) REFERENCES resource_slots (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;