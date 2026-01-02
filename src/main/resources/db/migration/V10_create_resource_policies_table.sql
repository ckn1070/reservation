CREATE TABLE resource_policies
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    resource_id  BIGINT         NOT NULL,

    policy_type  VARCHAR(40)    NOT NULL,                     -- 예: NO_SMOKING, AGE_LIMIT, GROUP_ONLY, MAX_CONCURRENT
    value_string VARCHAR(255)   NULL,
    value_number DECIMAL(19, 4) NULL,
    value_bool   BOOLEAN        NULL,

    created_at   TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_resource_policy (resource_id, policy_type), -- 동일 리소스, 같은 정책 타입 중복 방지
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
  COLLATE = utf8mb4_0900_ai_ci;