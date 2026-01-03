CREATE TABLE resources
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id     BIGINT       NULL,                   -- Resource Write 기준

    type          VARCHAR(20)  NOT NULL,               -- VENUE / FLOOR / ROW / SEAT ..
    code          VARCHAR(20)  NOT NULL,               -- 예: VA-1F-RA-01
    name          VARCHAR(120) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    capacity      INT          NOT NULL DEFAULT 1,     -- Resource 단위에서 동시에 수용 가능한 최대 인원 수 (SEAT=1, 상위 리소스는 해당 공간의 정원)
    is_reservable BOOLEAN      NOT NULL DEFAULT FALSE, -- 예약 가능한 단위 여부

    location_text VARCHAR(255) NULL,
    description   TEXT         NULL,

    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_resources_code (code),
    KEY idx_resources_parent (parent_id),
    KEY idx_resources_type (type),
    KEY idx_resources_status (status),

    CHECK (type IN ('VENUE', 'FLOOR', 'ROW', 'SEAT')),
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DELETED')),
    CHECK (capacity >= 1),
    CHECK (type <> 'SEAT' OR capacity = 1),

    CONSTRAINT fk_resources_parent
        FOREIGN KEY (parent_id) REFERENCES resources (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;