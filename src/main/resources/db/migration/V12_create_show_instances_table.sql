CREATE TABLE show_instances
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    resource_id    BIGINT       NOT NULL, -- 공연이 열리는 상위 리소스 (VENUE)

    title          VARCHAR(100) NOT NULL, -- 공연명
    start_at       TIMESTAMP(6) NOT NULL,
    end_at         TIMESTAMP(6) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    sales_open_at  TIMESTAMP(6) NULL,
    sales_close_at TIMESTAMP(6) NULL,

    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- 동일 공연장에서 같은 시간 회차 중복 방지
    UNIQUE KEY uk_show_instances_resource_time (resource_id, start_at, end_at),
    KEY idx_show_instances_time (start_at, end_at),
    KEY idx_show_instances_status (status),

    CHECK (start_at < end_at),
    CHECK (
        (sales_open_at IS NULL AND sales_close_at IS NULL)
            OR
        (sales_open_at IS NOT NULL AND sales_close_at IS NOT NULL
            AND sales_open_at < sales_close_at)
        ),
    CHECK (status IN ('SCHEDULED', 'OPEN', 'CLOSED', 'CANCELLED')),

    CONSTRAINT fk_show_instances_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
