CREATE TABLE resource_slot_lock_history
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    slot_id        BIGINT       NOT NULL COMMENT 'FK → resource_slots.id',
    reservation_id BIGINT       NOT NULL COMMENT 'FK → reservations.id',

    action         VARCHAR(20)  NOT NULL COMMENT '액션: HELD/CONFIRMED/RELEASED/EXPIRED/CANCELLED',
    reason         VARCHAR(255) NULL COMMENT '사유 (만료/취소 등)',

    held_at        TIMESTAMP(6) NULL COMMENT '최초 HELD 시각',
    expires_at     TIMESTAMP(6) NULL COMMENT 'HELD 당시 TTL',
    action_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '액션 수행 시각',

    PRIMARY KEY (id),

    KEY idx_lock_hist_slot_time (slot_id, action_at),
    KEY idx_lock_hist_reservation_time (reservation_id, action_at),
    KEY idx_lock_hist_action_time (action, action_at),

    CHECK (action IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED', 'CANCELLED')),

    CONSTRAINT fk_lock_hist_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_lock_hist_slot
        FOREIGN KEY (slot_id) REFERENCES resource_slots (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '잠금 이력 (감사 추적)';
