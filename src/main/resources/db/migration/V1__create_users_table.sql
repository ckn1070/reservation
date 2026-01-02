CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,

    email         VARCHAR(200) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    phone         VARCHAR(30)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    last_login_at TIMESTAMP(6) NULL,
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),


    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),

    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;