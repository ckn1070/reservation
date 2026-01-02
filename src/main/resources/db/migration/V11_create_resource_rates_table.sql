CREATE TABLE resource_rates
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    resource_id      BIGINT       NOT NULL,

    rate_type        VARCHAR(20)  NOT NULL DEFAULT 'BASE', -- BASE/OVERRIDE/PROMOTION 등
    -- 적용 기간: NULL이면 기본(상시) 요금
    start_at         TIMESTAMP(6) NULL,
    end_at           TIMESTAMP(6) NULL,
    base_default_key BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN rate_type = 'BASE' AND start_at IS NULL AND end_at IS NULL
                    THEN resource_id
                END
            ) STORED,
    amount           BIGINT       NOT NULL,
    currency         CHAR(3)      NOT NULL DEFAULT 'KRW',
    priority         INT          NOT NULL DEFAULT 0,
    reason           VARCHAR(255) NULL,

    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_resource_rates_base (base_default_key),
    KEY idx_resource_rates_lookup (resource_id, rate_type, start_at, end_at, priority),

    CHECK (amount >= 0),
    CHECK (rate_type IN ('BASE', 'OVERRIDE', 'PROMOTION')),
    CHECK (
        (start_at IS NULL AND end_at IS NULL)
            OR
        (start_at IS NOT NULL AND end_at IS NOT NULL AND start_at < end_at)
        ),

    CONSTRAINT fk_resource_rates_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;