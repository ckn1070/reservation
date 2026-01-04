CREATE TABLE seat_properties
(
    seat_id          BIGINT       NOT NULL,               -- resources.id (type=SEAT 앱에서 검증 필요)
    grade_id         BIGINT       NULL,

    has_power_outlet BOOLEAN      NOT NULL DEFAULT FALSE,
    is_accessible    BOOLEAN      NOT NULL DEFAULT FALSE, -- 휠체어석 등
    is_aisle         BOOLEAN      NOT NULL DEFAULT FALSE, -- 통로석
    is_window        BOOLEAN      NOT NULL DEFAULT FALSE, -- 창가
    view_score       INT          NULL,                   -- 시야 점수

    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

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
  COLLATE = utf8mb4_0900_ai_ci;