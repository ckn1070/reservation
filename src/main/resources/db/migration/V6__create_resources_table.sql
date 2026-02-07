CREATE TABLE resources
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    parent_id     BIGINT       NULL COMMENT '직접 상위 리소스 ID (쓰기용)',

    type          VARCHAR(20)  NOT NULL COMMENT '리소스 유형: VENUE/FLOOR/ROW/SEAT',
    code          VARCHAR(20)  NOT NULL COMMENT '부모 컨텍스트 내 고유 코드 (VENUE=VN001, FLOOR=1F, ROW=RA, SEAT=S1)',
    name          VARCHAR(120) NOT NULL COMMENT '리소스 표시 이름',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '리소스 상태: ACTIVE/INACTIVE/MAINTENANCE/DELETED',
    capacity      INT          NOT NULL DEFAULT 1 COMMENT '수용 인원 (SEAT=1 고정)',
    is_reservable BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '예약 가능 단위 여부 (SEAT만 TRUE)',

    location_text VARCHAR(255) NULL COMMENT '위치 설명',
    description   TEXT         NULL COMMENT '상세 설명',

    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_resources_parent_code (parent_id, code),
    KEY idx_resources_parent (parent_id),
    KEY idx_resources_type (type),
    KEY idx_resources_status (status),

    CHECK (type IN ('VENUE', 'FLOOR', 'ROW', 'SEAT')),
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DELETED')),
    CHECK (capacity >= 1),
    CHECK (type <> 'SEAT' OR capacity = 1),

    CONSTRAINT fk_resources_parent
        FOREIGN KEY (parent_id) REFERENCES resources (id)
            ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '계층적 리소스 (공연장/층/열/좌석)';
