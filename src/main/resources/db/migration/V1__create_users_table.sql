CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',

    email         VARCHAR(200) NOT NULL COMMENT '로그인 이메일 (UNIQUE)',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시 비밀번호',
    name          VARCHAR(50)  NOT NULL COMMENT '사용자 이름 (표시용)',
    phone         VARCHAR(30)  NOT NULL COMMENT '연락처 (예약 확인용)',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태: ACTIVE/SUSPENDED/DELETED',

    last_login_at TIMESTAMP(6) NULL COMMENT '마지막 로그인 시각',
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',


    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),

    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '사용자 계정';
