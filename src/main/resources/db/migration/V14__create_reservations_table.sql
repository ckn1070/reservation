CREATE TABLE reservations
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id          BIGINT       NOT NULL COMMENT 'FK → users.id',
    show_instance_id BIGINT       NOT NULL COMMENT 'FK → show_instances.id',

    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '예약 상태: PENDING/CONFIRMED/CANCELLED/NO_SHOW/COMPLETED',
    cancel_reason    VARCHAR(200) NULL COMMENT '취소 사유 (status=CANCELLED일 때만)',

    confirmed_at     TIMESTAMP(6) NULL COMMENT '확정 시각',
    cancelled_at     TIMESTAMP(6) NULL COMMENT '취소 시각',
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),

    KEY idx_reservations_show (show_instance_id),
    KEY idx_reservations_user_created (user_id, created_at),
    KEY idx_reservations_status (status),

    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'NO_SHOW', 'COMPLETED')),
    CHECK ((status <> 'CONFIRMED') OR (confirmed_at IS NOT NULL)),
    CHECK ((status <> 'CANCELLED') OR (cancelled_at IS NOT NULL)),
    CHECK ((cancel_reason IS NULL) OR (status = 'CANCELLED')),

    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reservations_show
        FOREIGN KEY (show_instance_id) REFERENCES show_instances (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '예약';
