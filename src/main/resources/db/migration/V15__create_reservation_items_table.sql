CREATE TABLE reservation_items
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    reservation_id BIGINT       NOT NULL COMMENT 'FK → reservations.id',
    slot_id        BIGINT       NOT NULL COMMENT 'FK → resource_slots.id',

    price_amount   BIGINT       NOT NULL COMMENT '예약 시점 가격 스냅샷 (원 단위)',
    currency       CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT '통화 코드 (ISO 4217)',

    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',

    PRIMARY KEY (id),

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
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '예약 항목 (불변)';
