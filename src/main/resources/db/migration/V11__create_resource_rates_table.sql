CREATE TABLE resource_rates
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    resource_id      BIGINT       NOT NULL COMMENT 'FK → resources.id',

    rate_type        VARCHAR(20)  NOT NULL DEFAULT 'BASE' COMMENT '요금 유형: BASE/OVERRIDE/PROMOTION',
    start_at         TIMESTAMP(6) NULL COMMENT '적용 시작 시각 (NULL이면 상시)',
    end_at           TIMESTAMP(6) NULL COMMENT '적용 종료 시각 (NULL이면 상시)',
    -- BASE 타입 + 기간 NULL인 경우만 resource_id 반환 → 리소스당 기본 요금 1개 보장
    base_default_key BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN rate_type = 'BASE' AND start_at IS NULL AND end_at IS NULL
                    THEN resource_id
                END
            ) STORED COMMENT '기본 요금 중복 방지용 Generated Column',
    amount           BIGINT       NOT NULL COMMENT '금액 (원 단위)',
    currency         CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT '통화 코드 (ISO 4217)',
    priority         INT          NOT NULL DEFAULT 0 COMMENT '우선순위 (높을수록 우선)',
    reason           VARCHAR(255) NULL COMMENT '가격 책정 사유',

    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

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
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '리소스 요금';
