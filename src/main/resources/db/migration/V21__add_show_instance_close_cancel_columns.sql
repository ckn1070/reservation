ALTER TABLE show_instances
    ADD COLUMN closed_at TIMESTAMP(6) NULL COMMENT '예매 마감 시각' AFTER sales_close_at,
    ADD COLUMN cancelled_at TIMESTAMP(6) NULL COMMENT '취소 시각' AFTER closed_at,
    ADD COLUMN cancel_reason VARCHAR(200) NULL COMMENT '취소 사유' AFTER cancelled_at;
