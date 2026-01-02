CREATE TABLE resource_slots
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    resource_id     BIGINT       NOT NULL,
    applied_rate_id BIGINT       NULL,

    start_at        TIMESTAMP(6) NOT NULL,
    end_at          TIMESTAMP(6) NOT NULL,

    currency        CHAR(3)      NOT NULL DEFAULT 'KRW',
    price_amount    BIGINT       NOT NULL,
    capacity        INT          NOT NULL DEFAULT 1,                            -- 좌석=1, 상위 리소스=정원
    reserved        INT          NOT NULL DEFAULT 0,                            -- 예약 확정 시 증가
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',                       -- OPEN/CLOSED

    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_resource_slots_resource_time (resource_id, start_at, end_at), -- 동일 리소스 동일 시간 슬롯 중복 생성 방지
    KEY idx_resource_slots_start (resource_id, start_at),
    KEY idx_resource_slots_time (start_at, end_at),

    CHECK (start_at < end_at),
    CHECK (price_amount >= 0),
    CHECK (capacity >= 1),
    CHECK (reserved >= 0),
    CHECK (reserved <= capacity),
    CHECK (status IN ('OPEN', 'CLOSED')),

    CONSTRAINT fk_resource_slots_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_resource_slots_rate
        FOREIGN KEY (applied_rate_id) REFERENCES resource_rates (id)
            ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;