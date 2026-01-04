CREATE TABLE resource_closure -- Resource Read 기준
(
    ancestor_id   BIGINT NOT NULL,
    descendant_id BIGINT NOT NULL,
    depth         INT    NOT NULL,

    PRIMARY KEY (ancestor_id, descendant_id),
    KEY idx_resource_closure_desc (descendant_id), -- 상위 리소스 빠르게 탐색

    CHECK (depth >= 0),

    CONSTRAINT fk_resource_closure_ancestor
        FOREIGN KEY (ancestor_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_resource_closure_descendant
        FOREIGN KEY (descendant_id) REFERENCES resources (id)
            ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;