CREATE TABLE seat_grades
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,

    grade_code VARCHAR(30)  NOT NULL, -- VIP / R / S / A 등
    grade_name VARCHAR(50)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_grades_code (grade_code),
    KEY idx_seat_grades_sort (sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;