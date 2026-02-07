CREATE TABLE seat_grades
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',

    grade_code VARCHAR(30)  NOT NULL COMMENT '등급 코드: VIP/R/S/A 등',
    grade_name VARCHAR(50)  NOT NULL COMMENT '등급 이름: VIP석/R석 등',
    sort_order INT          NOT NULL DEFAULT 0 COMMENT '정렬 순서 (낮을수록 상위)',

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_grades_code (grade_code),
    KEY idx_seat_grades_sort (sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '좌석 등급';
