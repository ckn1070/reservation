CREATE TABLE resource_slots
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    show_instance_id BIGINT       NOT NULL,
    seat_id          BIGINT       NOT NULL,                        -- resources.type = SEAT
    applied_rate_id  BIGINT       NULL,

    currency         CHAR(3)      NOT NULL DEFAULT 'KRW',
    price_amount     BIGINT       NOT NULL,

    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',         -- OPEN/CLOSED

    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_resource_slots_show (show_instance_id, seat_id), -- 회차+좌석 1개 슬롯 고정
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
  COLLATE = utf8mb4_0900_ai_ci;