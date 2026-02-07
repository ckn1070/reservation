CREATE TABLE show_instances
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    resource_id    BIGINT       NOT NULL COMMENT 'FK → resources.id (VENUE)',

    title          VARCHAR(100) NOT NULL COMMENT '공연명',
    start_at       TIMESTAMP(6) NOT NULL COMMENT '공연 시작 시각',
    end_at         TIMESTAMP(6) NOT NULL COMMENT '공연 종료 시각',
    status         VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED' COMMENT '회차 상태: SCHEDULED/OPEN/CLOSED/CANCELLED',
    sales_open_at  TIMESTAMP(6) NULL COMMENT '예매 오픈 시각',
    sales_close_at TIMESTAMP(6) NULL COMMENT '예매 마감 시각',

    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),

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
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '공연 회차';
