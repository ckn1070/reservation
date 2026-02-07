CREATE TABLE seat_properties
(
    seat_id          BIGINT       NOT NULL COMMENT 'PK, FK → resources.id (type=SEAT, 앱에서 검증)',
    grade_id         BIGINT       NULL COMMENT 'FK → seat_grades.id',

    has_power_outlet BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '콘센트 유무',
    is_accessible    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '휠체어석 여부',
    is_aisle         BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '통로석 여부',
    is_window        BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '창가석 여부',
    view_score       INT          NULL COMMENT '시야 점수 (0~100)',

    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (seat_id),
    KEY idx_seat_props_grade (grade_id),
    KEY idx_seat_props_power (has_power_outlet),
    KEY idx_seat_props_access (is_accessible),
    KEY idx_seat_props_aisle (is_aisle),

    CHECK (view_score IS NULL OR (view_score >= 0 AND view_score <= 100)),

    CONSTRAINT fk_seat_props_seat
        FOREIGN KEY (seat_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,

    CONSTRAINT fk_seat_props_grade
        FOREIGN KEY (grade_id) REFERENCES seat_grades (id)
            ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '좌석 속성 (1:1)';
