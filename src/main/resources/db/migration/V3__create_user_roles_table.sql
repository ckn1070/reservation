CREATE TABLE user_roles
(
    user_id    BIGINT       NOT NULL,
    role_id    BIGINT       NOT NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),


    PRIMARY KEY (user_id, role_id),
    
    CONSTRAINT fk_user_roles_users
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_user_roles_roles
        FOREIGN KEY (role_id) REFERENCES roles (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;