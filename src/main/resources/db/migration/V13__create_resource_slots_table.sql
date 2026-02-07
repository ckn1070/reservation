CREATE TABLE resource_slots
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    show_instance_id BIGINT       NOT NULL COMMENT 'FK → show_instances.id',
    seat_id          BIGINT       NOT NULL COMMENT 'FK → resources.id (type=SEAT)',
    applied_rate_id  BIGINT       NULL COMMENT 'FK → resource_rates.id',

    currency         CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT '통화 코드 (ISO 4217)',
    price_amount     BIGINT       NOT NULL COMMENT '적용 가격 (원 단위)',

    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT '슬롯 상태: OPEN/CLOSED',

    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),

    UNIQUE KEY uk_resource_slots_show (show_instance_id, seat_id),
    KEY idx_resource_slots_show (show_instance_id, status),
    KEY idx_resource_slots_seat (seat_id, status),

    CHECK (price_amount >= 0),
    CHECK (status IN ('OPEN', 'CLOSED')),

    CONSTRAINT fk_resource_slots_show
        FOREIGN KEY (show_instance_id) REFERENCES show_instances (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_resource_slots_resource
        FOREIGN KEY (seat_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_resource_slots_rate
        FOREIGN KEY (applied_rate_id) REFERENCES resource_rates (id)
            ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '예약 가능 슬롯 (회차 + 좌석)';
