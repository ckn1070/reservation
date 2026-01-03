CREATE TABLE resource_slot_locks
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    slot_id        BIGINT       NOT NULL,
    reservation_id BIGINT       NOT NULL,                -- 락 소유 예약 ID

    status         VARCHAR(20)  NOT NULL DEFAULT 'HELD', -- HELD: 결제 대기 선점 / CONFIRMED: 확정 점유

    held_at        TIMESTAMP(6) NOT NULL,
    expires_at     TIMESTAMP(6) NULL,                    -- HELD일 경우 필요 (TTL)
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_lock_slot (slot_id),

    KEY idx_lock_reservation (reservation_id),           -- 예약에서 탐색 (결제 확정/취소 시 락 처리)
    KEY idx_lock_status_expires (status, expires_at),    -- 만료 락 정리 (배치/스케줄러)

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
  COLLATE = utf8mb4_0900_ai_ci;
