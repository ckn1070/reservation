CREATE TABLE reservations
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    show_instance_id BIGINT       NOT NULL,

    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- 대표 시간
    cancel_reason    VARCHAR(200) NULL,

    confirmed_at     TIMESTAMP(6) NULL,
    cancelled_at     TIMESTAMP(6) NULL,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    KEY idx_reservations_show (show_instance_id),
    KEY idx_reservations_user_created (user_id, created_at),
    KEY idx_reservations_status (status),

    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'NO_SHOW', 'COMPLETED')),
    CHECK ((status <> 'CONFIRMED') OR (confirmed_at IS NOT NULL)),
    CHECK ((status <> 'CANCELLED') OR (cancelled_at IS NOT NULL)),
    CHECK ((cancel_reason IS NULL) OR (status = 'CANCELLED')),

    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reservations_show
        FOREIGN KEY (show_instance_id) REFERENCES show_instances (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;