-- 초기 SUPER_ADMIN 계정 생성
-- 이메일: admin@drlom.com
-- 초기 비밀번호: PasswordInit (배포 후 즉시 변경 필수)
-- is_password_change_required: true (최초 로그인 시 비밀번호 변경 강제)

INSERT INTO users (email, password_hash, name, phone, status, is_password_change_required)
VALUES ('admin@drlom.com',
        '$2a$10$DXojOk4V4lvEtK1l4YARTOjI1gHPYGgjOO7Z2pPGc/EK.xIUzbVFO',
        'Super Admin',
        '010-1234-5678',
        'ACTIVE',
        TRUE);

-- SUPER_ADMIN 역할 부여
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u,
     roles r
WHERE u.email = 'admin@drlom.com'
  AND r.name = 'ROLE_SUPER_ADMIN';
