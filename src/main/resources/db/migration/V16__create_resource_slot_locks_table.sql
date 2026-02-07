CREATE TABLE resource_slot_locks
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    slot_id        BIGINT       NOT NULL COMMENT 'FK → resource_slots.id (UNIQUE: 슬롯당 1개 락)',
    reservation_id BIGINT       NOT NULL COMMENT 'FK → reservations.id (락 소유 예약)',

    status         VARCHAR(20)  NOT NULL DEFAULT 'HELD' COMMENT '잠금 상태: HELD(결제 대기)/CONFIRMED(확정 점유)',

    held_at        TIMESTAMP(6) NOT NULL COMMENT '잠금 시작 시각',
    expires_at     TIMESTAMP(6) NULL COMMENT '만료 시각 (HELD만 필수, TTL)',
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_lock_slot (slot_id),

    KEY idx_lock_reservation (reservation_id),
    KEY idx_lock_status_expires (status, expires_at),

    CHECK (status IN ('HELD', 'CONFIRMED')),
    CHECK ((status <> 'HELD') OR (expires_at IS NOT NULL)),
    CHECK ((status <> 'CONFIRMED') OR (expires_at IS NULL)),
    CHECK ((expires_at IS NULL) OR (held_at <= expires_at)),

    CONSTRAINT fk_lock_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_lock_slot
        FOREIGN KEY (slot_id) REFERENCES resource_slots (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '슬롯 잠금 (동시성 제어)';
