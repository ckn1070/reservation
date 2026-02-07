-- 임시 비밀번호로 생성된 관리자는 최초 로그인 시 비밀번호 변경 필수
ALTER TABLE users
    ADD COLUMN is_password_change_required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '비밀번호 변경 필요 여부 (임시 비밀번호 발급 시 TRUE)';
