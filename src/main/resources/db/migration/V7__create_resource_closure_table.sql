CREATE TABLE resource_closure
(
    ancestor_id   BIGINT NOT NULL COMMENT '조상 리소스 ID',
    descendant_id BIGINT NOT NULL COMMENT '자손 리소스 ID',
    depth         INT    NOT NULL COMMENT '계층 깊이 (자기 자신=0)',

    PRIMARY KEY (ancestor_id, descendant_id),
    KEY idx_resource_closure_desc (descendant_id) COMMENT '상위 리소스 탐색용',

    CHECK (depth >= 0),

    CONSTRAINT fk_resource_closure_ancestor
        FOREIGN KEY (ancestor_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_resource_closure_descendant
        FOREIGN KEY (descendant_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Closure Table - 리소스 계층 관계 (읽기용)';
