CREATE TABLE refresh_tokens
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_id    BIGINT       NOT NULL COMMENT 'FK → users.id',

    token_hash BINARY(32)   NOT NULL COMMENT 'SHA-256 해시 (원본 토큰 미저장)',
    issued_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '발급 시각',
    expires_at TIMESTAMP(6) NOT NULL COMMENT '만료 시각',
    revoked_at TIMESTAMP(6) NULL COMMENT '폐기 시각 (NULL이면 유효)',

    device_id  VARCHAR(100) NULL COMMENT '디바이스 식별자',
    ip         VARCHAR(45)  NULL COMMENT '발급 시 IP',
    user_agent VARCHAR(255) NULL COMMENT '브라우저 정보',

    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expires (expires_at),
    KEY idx_refresh_tokens_revoked (revoked_at),
    KEY idx_refresh_tokens_lookup (token_hash, revoked_at, expires_at),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'JWT Refresh Token 관리';
