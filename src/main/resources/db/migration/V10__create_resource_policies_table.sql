CREATE TABLE resource_policies
(
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'PK',
    resource_id  BIGINT         NOT NULL COMMENT 'FK → resources.id',

    policy_type  VARCHAR(40)    NOT NULL COMMENT '정책 유형: NO_SMOKING/AGE_LIMIT/GROUP_ONLY/MAX_CONCURRENT 등',
    value_string VARCHAR(255)   NULL COMMENT '문자열 값',
    value_number DECIMAL(19, 4) NULL COMMENT '숫자 값',
    value_bool   BOOLEAN        NULL COMMENT '불리언 값',

    created_at   TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at   TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_policy (resource_id, policy_type),
    KEY idx_policy_type (policy_type),

    -- 값 컬럼 중 하나만 사용
    CHECK (
        (value_string IS NOT NULL AND value_number IS NULL AND value_bool IS NULL)
            OR
        (value_string IS NULL AND value_number IS NOT NULL AND value_bool IS NULL)
            OR
        (value_string IS NULL AND value_number IS NULL AND value_bool IS NOT NULL)
            OR
        (value_string IS NULL AND value_number IS NULL AND value_bool IS NULL)
        ),

    CONSTRAINT fk_resource_policies_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '리소스 정책 (EAV 패턴)';
