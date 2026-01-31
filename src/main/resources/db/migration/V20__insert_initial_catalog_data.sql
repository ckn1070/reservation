-- ============================================================
-- V20: 초기 카탈로그 데이터 (소규모 공연장 100석)
-- ============================================================
-- 구조: 소극장 → 1층 → A~J열 → 각 열 10좌석
-- 등급: VIP(A,B열), R(C,D열), S(E,F,G열), A(H,I,J열)
-- ============================================================

-- ------------------------------------------------------------
-- 1. 좌석 등급 (seat_grades)
-- ------------------------------------------------------------
INSERT INTO seat_grades (grade_code, grade_name, sort_order)
VALUES ('VIP', 'VIP석', 1),
       ('R', 'R석', 2),
       ('S', 'S석', 3),
       ('A', 'A석', 4);

-- ------------------------------------------------------------
-- 2. 리소스 (resources) - 계층 구조
-- ------------------------------------------------------------

-- 2.1 공연장 (VENUE)
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable, location_text, description)
VALUES (1, NULL, 'VENUE', 'VENUE-001', '소극장', 'ACTIVE', 100, FALSE, '서울시 중구 예술로 777', 'Default VENUE 소극장');

-- 2.2 층 (FLOOR)
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (2, 1, 'FLOOR', 'FLOOR-1F', '1층', 'ACTIVE', 100, FALSE);

-- 2.3 열 (ROW) - A~J열
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (3, 2, 'ROW', 'ROW-A', 'A열', 'ACTIVE', 10, FALSE),
       (4, 2, 'ROW', 'ROW-B', 'B열', 'ACTIVE', 10, FALSE),
       (5, 2, 'ROW', 'ROW-C', 'C열', 'ACTIVE', 10, FALSE),
       (6, 2, 'ROW', 'ROW-D', 'D열', 'ACTIVE', 10, FALSE),
       (7, 2, 'ROW', 'ROW-E', 'E열', 'ACTIVE', 10, FALSE),
       (8, 2, 'ROW', 'ROW-F', 'F열', 'ACTIVE', 10, FALSE),
       (9, 2, 'ROW', 'ROW-G', 'G열', 'ACTIVE', 10, FALSE),
       (10, 2, 'ROW', 'ROW-H', 'H열', 'ACTIVE', 10, FALSE),
       (11, 2, 'ROW', 'ROW-I', 'I열', 'ACTIVE', 10, FALSE),
       (12, 2, 'ROW', 'ROW-J', 'J열', 'ACTIVE', 10, FALSE);

-- 2.4 좌석 (SEAT) - 각 열당 10개
-- A열 (id: 13~22) - VIP
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (13, 3, 'SEAT', 'SEAT-A1', 'A1', 'ACTIVE', 1, TRUE),
       (14, 3, 'SEAT', 'SEAT-A2', 'A2', 'ACTIVE', 1, TRUE),
       (15, 3, 'SEAT', 'SEAT-A3', 'A3', 'ACTIVE', 1, TRUE),
       (16, 3, 'SEAT', 'SEAT-A4', 'A4', 'ACTIVE', 1, TRUE),
       (17, 3, 'SEAT', 'SEAT-A5', 'A5', 'ACTIVE', 1, TRUE),
       (18, 3, 'SEAT', 'SEAT-A6', 'A6', 'ACTIVE', 1, TRUE),
       (19, 3, 'SEAT', 'SEAT-A7', 'A7', 'ACTIVE', 1, TRUE),
       (20, 3, 'SEAT', 'SEAT-A8', 'A8', 'ACTIVE', 1, TRUE),
       (21, 3, 'SEAT', 'SEAT-A9', 'A9', 'ACTIVE', 1, TRUE),
       (22, 3, 'SEAT', 'SEAT-A10', 'A10', 'ACTIVE', 1, TRUE);

-- B열 (id: 23~32) - VIP
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (23, 4, 'SEAT', 'SEAT-B1', 'B1', 'ACTIVE', 1, TRUE),
       (24, 4, 'SEAT', 'SEAT-B2', 'B2', 'ACTIVE', 1, TRUE),
       (25, 4, 'SEAT', 'SEAT-B3', 'B3', 'ACTIVE', 1, TRUE),
       (26, 4, 'SEAT', 'SEAT-B4', 'B4', 'ACTIVE', 1, TRUE),
       (27, 4, 'SEAT', 'SEAT-B5', 'B5', 'ACTIVE', 1, TRUE),
       (28, 4, 'SEAT', 'SEAT-B6', 'B6', 'ACTIVE', 1, TRUE),
       (29, 4, 'SEAT', 'SEAT-B7', 'B7', 'ACTIVE', 1, TRUE),
       (30, 4, 'SEAT', 'SEAT-B8', 'B8', 'ACTIVE', 1, TRUE),
       (31, 4, 'SEAT', 'SEAT-B9', 'B9', 'ACTIVE', 1, TRUE),
       (32, 4, 'SEAT', 'SEAT-B10', 'B10', 'ACTIVE', 1, TRUE);

-- C열 (id: 33~42) - R
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (33, 5, 'SEAT', 'SEAT-C1', 'C1', 'ACTIVE', 1, TRUE),
       (34, 5, 'SEAT', 'SEAT-C2', 'C2', 'ACTIVE', 1, TRUE),
       (35, 5, 'SEAT', 'SEAT-C3', 'C3', 'ACTIVE', 1, TRUE),
       (36, 5, 'SEAT', 'SEAT-C4', 'C4', 'ACTIVE', 1, TRUE),
       (37, 5, 'SEAT', 'SEAT-C5', 'C5', 'ACTIVE', 1, TRUE),
       (38, 5, 'SEAT', 'SEAT-C6', 'C6', 'ACTIVE', 1, TRUE),
       (39, 5, 'SEAT', 'SEAT-C7', 'C7', 'ACTIVE', 1, TRUE),
       (40, 5, 'SEAT', 'SEAT-C8', 'C8', 'ACTIVE', 1, TRUE),
       (41, 5, 'SEAT', 'SEAT-C9', 'C9', 'ACTIVE', 1, TRUE),
       (42, 5, 'SEAT', 'SEAT-C10', 'C10', 'ACTIVE', 1, TRUE);

-- D열 (id: 43~52) - R
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (43, 6, 'SEAT', 'SEAT-D1', 'D1', 'ACTIVE', 1, TRUE),
       (44, 6, 'SEAT', 'SEAT-D2', 'D2', 'ACTIVE', 1, TRUE),
       (45, 6, 'SEAT', 'SEAT-D3', 'D3', 'ACTIVE', 1, TRUE),
       (46, 6, 'SEAT', 'SEAT-D4', 'D4', 'ACTIVE', 1, TRUE),
       (47, 6, 'SEAT', 'SEAT-D5', 'D5', 'ACTIVE', 1, TRUE),
       (48, 6, 'SEAT', 'SEAT-D6', 'D6', 'ACTIVE', 1, TRUE),
       (49, 6, 'SEAT', 'SEAT-D7', 'D7', 'ACTIVE', 1, TRUE),
       (50, 6, 'SEAT', 'SEAT-D8', 'D8', 'ACTIVE', 1, TRUE),
       (51, 6, 'SEAT', 'SEAT-D9', 'D9', 'ACTIVE', 1, TRUE),
       (52, 6, 'SEAT', 'SEAT-D10', 'D10', 'ACTIVE', 1, TRUE);

-- E열 (id: 53~62) - S
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (53, 7, 'SEAT', 'SEAT-E1', 'E1', 'ACTIVE', 1, TRUE),
       (54, 7, 'SEAT', 'SEAT-E2', 'E2', 'ACTIVE', 1, TRUE),
       (55, 7, 'SEAT', 'SEAT-E3', 'E3', 'ACTIVE', 1, TRUE),
       (56, 7, 'SEAT', 'SEAT-E4', 'E4', 'ACTIVE', 1, TRUE),
       (57, 7, 'SEAT', 'SEAT-E5', 'E5', 'ACTIVE', 1, TRUE),
       (58, 7, 'SEAT', 'SEAT-E6', 'E6', 'ACTIVE', 1, TRUE),
       (59, 7, 'SEAT', 'SEAT-E7', 'E7', 'ACTIVE', 1, TRUE),
       (60, 7, 'SEAT', 'SEAT-E8', 'E8', 'ACTIVE', 1, TRUE),
       (61, 7, 'SEAT', 'SEAT-E9', 'E9', 'ACTIVE', 1, TRUE),
       (62, 7, 'SEAT', 'SEAT-E10', 'E10', 'ACTIVE', 1, TRUE);

-- F열 (id: 63~72) - S
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (63, 8, 'SEAT', 'SEAT-F1', 'F1', 'ACTIVE', 1, TRUE),
       (64, 8, 'SEAT', 'SEAT-F2', 'F2', 'ACTIVE', 1, TRUE),
       (65, 8, 'SEAT', 'SEAT-F3', 'F3', 'ACTIVE', 1, TRUE),
       (66, 8, 'SEAT', 'SEAT-F4', 'F4', 'ACTIVE', 1, TRUE),
       (67, 8, 'SEAT', 'SEAT-F5', 'F5', 'ACTIVE', 1, TRUE),
       (68, 8, 'SEAT', 'SEAT-F6', 'F6', 'ACTIVE', 1, TRUE),
       (69, 8, 'SEAT', 'SEAT-F7', 'F7', 'ACTIVE', 1, TRUE),
       (70, 8, 'SEAT', 'SEAT-F8', 'F8', 'ACTIVE', 1, TRUE),
       (71, 8, 'SEAT', 'SEAT-F9', 'F9', 'ACTIVE', 1, TRUE),
       (72, 8, 'SEAT', 'SEAT-F10', 'F10', 'ACTIVE', 1, TRUE);

-- G열 (id: 73~82) - S
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (73, 9, 'SEAT', 'SEAT-G1', 'G1', 'ACTIVE', 1, TRUE),
       (74, 9, 'SEAT', 'SEAT-G2', 'G2', 'ACTIVE', 1, TRUE),
       (75, 9, 'SEAT', 'SEAT-G3', 'G3', 'ACTIVE', 1, TRUE),
       (76, 9, 'SEAT', 'SEAT-G4', 'G4', 'ACTIVE', 1, TRUE),
       (77, 9, 'SEAT', 'SEAT-G5', 'G5', 'ACTIVE', 1, TRUE),
       (78, 9, 'SEAT', 'SEAT-G6', 'G6', 'ACTIVE', 1, TRUE),
       (79, 9, 'SEAT', 'SEAT-G7', 'G7', 'ACTIVE', 1, TRUE),
       (80, 9, 'SEAT', 'SEAT-G8', 'G8', 'ACTIVE', 1, TRUE),
       (81, 9, 'SEAT', 'SEAT-G9', 'G9', 'ACTIVE', 1, TRUE),
       (82, 9, 'SEAT', 'SEAT-G10', 'G10', 'ACTIVE', 1, TRUE);

-- H열 (id: 83~92) - A
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (83, 10, 'SEAT', 'SEAT-H1', 'H1', 'ACTIVE', 1, TRUE),
       (84, 10, 'SEAT', 'SEAT-H2', 'H2', 'ACTIVE', 1, TRUE),
       (85, 10, 'SEAT', 'SEAT-H3', 'H3', 'ACTIVE', 1, TRUE),
       (86, 10, 'SEAT', 'SEAT-H4', 'H4', 'ACTIVE', 1, TRUE),
       (87, 10, 'SEAT', 'SEAT-H5', 'H5', 'ACTIVE', 1, TRUE),
       (88, 10, 'SEAT', 'SEAT-H6', 'H6', 'ACTIVE', 1, TRUE),
       (89, 10, 'SEAT', 'SEAT-H7', 'H7', 'ACTIVE', 1, TRUE),
       (90, 10, 'SEAT', 'SEAT-H8', 'H8', 'ACTIVE', 1, TRUE),
       (91, 10, 'SEAT', 'SEAT-H9', 'H9', 'ACTIVE', 1, TRUE),
       (92, 10, 'SEAT', 'SEAT-H10', 'H10', 'ACTIVE', 1, TRUE);

-- I열 (id: 93~102) - A
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (93, 11, 'SEAT', 'SEAT-I1', 'I1', 'ACTIVE', 1, TRUE),
       (94, 11, 'SEAT', 'SEAT-I2', 'I2', 'ACTIVE', 1, TRUE),
       (95, 11, 'SEAT', 'SEAT-I3', 'I3', 'ACTIVE', 1, TRUE),
       (96, 11, 'SEAT', 'SEAT-I4', 'I4', 'ACTIVE', 1, TRUE),
       (97, 11, 'SEAT', 'SEAT-I5', 'I5', 'ACTIVE', 1, TRUE),
       (98, 11, 'SEAT', 'SEAT-I6', 'I6', 'ACTIVE', 1, TRUE),
       (99, 11, 'SEAT', 'SEAT-I7', 'I7', 'ACTIVE', 1, TRUE),
       (100, 11, 'SEAT', 'SEAT-I8', 'I8', 'ACTIVE', 1, TRUE),
       (101, 11, 'SEAT', 'SEAT-I9', 'I9', 'ACTIVE', 1, TRUE),
       (102, 11, 'SEAT', 'SEAT-I10', 'I10', 'ACTIVE', 1, TRUE);

-- J열 (id: 103~112) - A
INSERT INTO resources (id, parent_id, type, code, name, status, capacity, is_reservable)
VALUES (103, 12, 'SEAT', 'SEAT-J1', 'J1', 'ACTIVE', 1, TRUE),
       (104, 12, 'SEAT', 'SEAT-J2', 'J2', 'ACTIVE', 1, TRUE),
       (105, 12, 'SEAT', 'SEAT-J3', 'J3', 'ACTIVE', 1, TRUE),
       (106, 12, 'SEAT', 'SEAT-J4', 'J4', 'ACTIVE', 1, TRUE),
       (107, 12, 'SEAT', 'SEAT-J5', 'J5', 'ACTIVE', 1, TRUE),
       (108, 12, 'SEAT', 'SEAT-J6', 'J6', 'ACTIVE', 1, TRUE),
       (109, 12, 'SEAT', 'SEAT-J7', 'J7', 'ACTIVE', 1, TRUE),
       (110, 12, 'SEAT', 'SEAT-J8', 'J8', 'ACTIVE', 1, TRUE),
       (111, 12, 'SEAT', 'SEAT-J9', 'J9', 'ACTIVE', 1, TRUE),
       (112, 12, 'SEAT', 'SEAT-J10', 'J10', 'ACTIVE', 1, TRUE);

-- ------------------------------------------------------------
-- 3. Closure Table (resource_closure) - 계층 관계
-- ------------------------------------------------------------

-- 3.1 VENUE 자기 참조
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (1, 1, 0);

-- 3.2 FLOOR (depth 0: 자기, depth 1: VENUE)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (2, 2, 0),
       (1, 2, 1);

-- 3.3 ROW (depth 0: 자기, depth 1: FLOOR, depth 2: VENUE)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES -- A열
       (3, 3, 0),
       (2, 3, 1),
       (1, 3, 2),
       -- B열
       (4, 4, 0),
       (2, 4, 1),
       (1, 4, 2),
       -- C열
       (5, 5, 0),
       (2, 5, 1),
       (1, 5, 2),
       -- D열
       (6, 6, 0),
       (2, 6, 1),
       (1, 6, 2),
       -- E열
       (7, 7, 0),
       (2, 7, 1),
       (1, 7, 2),
       -- F열
       (8, 8, 0),
       (2, 8, 1),
       (1, 8, 2),
       -- G열
       (9, 9, 0),
       (2, 9, 1),
       (1, 9, 2),
       -- H열
       (10, 10, 0),
       (2, 10, 1),
       (1, 10, 2),
       -- I열
       (11, 11, 0),
       (2, 11, 1),
       (1, 11, 2),
       -- J열
       (12, 12, 0),
       (2, 12, 1),
       (1, 12, 2);

-- 3.4 SEAT (depth 0: 자기, depth 1: ROW, depth 2: FLOOR, depth 3: VENUE)
-- A열 좌석 (id: 13~22, row: 3)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (13, 13, 0),
       (3, 13, 1),
       (2, 13, 2),
       (1, 13, 3),
       (14, 14, 0),
       (3, 14, 1),
       (2, 14, 2),
       (1, 14, 3),
       (15, 15, 0),
       (3, 15, 1),
       (2, 15, 2),
       (1, 15, 3),
       (16, 16, 0),
       (3, 16, 1),
       (2, 16, 2),
       (1, 16, 3),
       (17, 17, 0),
       (3, 17, 1),
       (2, 17, 2),
       (1, 17, 3),
       (18, 18, 0),
       (3, 18, 1),
       (2, 18, 2),
       (1, 18, 3),
       (19, 19, 0),
       (3, 19, 1),
       (2, 19, 2),
       (1, 19, 3),
       (20, 20, 0),
       (3, 20, 1),
       (2, 20, 2),
       (1, 20, 3),
       (21, 21, 0),
       (3, 21, 1),
       (2, 21, 2),
       (1, 21, 3),
       (22, 22, 0),
       (3, 22, 1),
       (2, 22, 2),
       (1, 22, 3);

-- B열 좌석 (id: 23~32, row: 4)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (23, 23, 0),
       (4, 23, 1),
       (2, 23, 2),
       (1, 23, 3),
       (24, 24, 0),
       (4, 24, 1),
       (2, 24, 2),
       (1, 24, 3),
       (25, 25, 0),
       (4, 25, 1),
       (2, 25, 2),
       (1, 25, 3),
       (26, 26, 0),
       (4, 26, 1),
       (2, 26, 2),
       (1, 26, 3),
       (27, 27, 0),
       (4, 27, 1),
       (2, 27, 2),
       (1, 27, 3),
       (28, 28, 0),
       (4, 28, 1),
       (2, 28, 2),
       (1, 28, 3),
       (29, 29, 0),
       (4, 29, 1),
       (2, 29, 2),
       (1, 29, 3),
       (30, 30, 0),
       (4, 30, 1),
       (2, 30, 2),
       (1, 30, 3),
       (31, 31, 0),
       (4, 31, 1),
       (2, 31, 2),
       (1, 31, 3),
       (32, 32, 0),
       (4, 32, 1),
       (2, 32, 2),
       (1, 32, 3);

-- C열 좌석 (id: 33~42, row: 5)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (33, 33, 0),
       (5, 33, 1),
       (2, 33, 2),
       (1, 33, 3),
       (34, 34, 0),
       (5, 34, 1),
       (2, 34, 2),
       (1, 34, 3),
       (35, 35, 0),
       (5, 35, 1),
       (2, 35, 2),
       (1, 35, 3),
       (36, 36, 0),
       (5, 36, 1),
       (2, 36, 2),
       (1, 36, 3),
       (37, 37, 0),
       (5, 37, 1),
       (2, 37, 2),
       (1, 37, 3),
       (38, 38, 0),
       (5, 38, 1),
       (2, 38, 2),
       (1, 38, 3),
       (39, 39, 0),
       (5, 39, 1),
       (2, 39, 2),
       (1, 39, 3),
       (40, 40, 0),
       (5, 40, 1),
       (2, 40, 2),
       (1, 40, 3),
       (41, 41, 0),
       (5, 41, 1),
       (2, 41, 2),
       (1, 41, 3),
       (42, 42, 0),
       (5, 42, 1),
       (2, 42, 2),
       (1, 42, 3);

-- D열 좌석 (id: 43~52, row: 6)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (43, 43, 0),
       (6, 43, 1),
       (2, 43, 2),
       (1, 43, 3),
       (44, 44, 0),
       (6, 44, 1),
       (2, 44, 2),
       (1, 44, 3),
       (45, 45, 0),
       (6, 45, 1),
       (2, 45, 2),
       (1, 45, 3),
       (46, 46, 0),
       (6, 46, 1),
       (2, 46, 2),
       (1, 46, 3),
       (47, 47, 0),
       (6, 47, 1),
       (2, 47, 2),
       (1, 47, 3),
       (48, 48, 0),
       (6, 48, 1),
       (2, 48, 2),
       (1, 48, 3),
       (49, 49, 0),
       (6, 49, 1),
       (2, 49, 2),
       (1, 49, 3),
       (50, 50, 0),
       (6, 50, 1),
       (2, 50, 2),
       (1, 50, 3),
       (51, 51, 0),
       (6, 51, 1),
       (2, 51, 2),
       (1, 51, 3),
       (52, 52, 0),
       (6, 52, 1),
       (2, 52, 2),
       (1, 52, 3);

-- E열 좌석 (id: 53~62, row: 7)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (53, 53, 0),
       (7, 53, 1),
       (2, 53, 2),
       (1, 53, 3),
       (54, 54, 0),
       (7, 54, 1),
       (2, 54, 2),
       (1, 54, 3),
       (55, 55, 0),
       (7, 55, 1),
       (2, 55, 2),
       (1, 55, 3),
       (56, 56, 0),
       (7, 56, 1),
       (2, 56, 2),
       (1, 56, 3),
       (57, 57, 0),
       (7, 57, 1),
       (2, 57, 2),
       (1, 57, 3),
       (58, 58, 0),
       (7, 58, 1),
       (2, 58, 2),
       (1, 58, 3),
       (59, 59, 0),
       (7, 59, 1),
       (2, 59, 2),
       (1, 59, 3),
       (60, 60, 0),
       (7, 60, 1),
       (2, 60, 2),
       (1, 60, 3),
       (61, 61, 0),
       (7, 61, 1),
       (2, 61, 2),
       (1, 61, 3),
       (62, 62, 0),
       (7, 62, 1),
       (2, 62, 2),
       (1, 62, 3);

-- F열 좌석 (id: 63~72, row: 8)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (63, 63, 0),
       (8, 63, 1),
       (2, 63, 2),
       (1, 63, 3),
       (64, 64, 0),
       (8, 64, 1),
       (2, 64, 2),
       (1, 64, 3),
       (65, 65, 0),
       (8, 65, 1),
       (2, 65, 2),
       (1, 65, 3),
       (66, 66, 0),
       (8, 66, 1),
       (2, 66, 2),
       (1, 66, 3),
       (67, 67, 0),
       (8, 67, 1),
       (2, 67, 2),
       (1, 67, 3),
       (68, 68, 0),
       (8, 68, 1),
       (2, 68, 2),
       (1, 68, 3),
       (69, 69, 0),
       (8, 69, 1),
       (2, 69, 2),
       (1, 69, 3),
       (70, 70, 0),
       (8, 70, 1),
       (2, 70, 2),
       (1, 70, 3),
       (71, 71, 0),
       (8, 71, 1),
       (2, 71, 2),
       (1, 71, 3),
       (72, 72, 0),
       (8, 72, 1),
       (2, 72, 2),
       (1, 72, 3);

-- G열 좌석 (id: 73~82, row: 9)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (73, 73, 0),
       (9, 73, 1),
       (2, 73, 2),
       (1, 73, 3),
       (74, 74, 0),
       (9, 74, 1),
       (2, 74, 2),
       (1, 74, 3),
       (75, 75, 0),
       (9, 75, 1),
       (2, 75, 2),
       (1, 75, 3),
       (76, 76, 0),
       (9, 76, 1),
       (2, 76, 2),
       (1, 76, 3),
       (77, 77, 0),
       (9, 77, 1),
       (2, 77, 2),
       (1, 77, 3),
       (78, 78, 0),
       (9, 78, 1),
       (2, 78, 2),
       (1, 78, 3),
       (79, 79, 0),
       (9, 79, 1),
       (2, 79, 2),
       (1, 79, 3),
       (80, 80, 0),
       (9, 80, 1),
       (2, 80, 2),
       (1, 80, 3),
       (81, 81, 0),
       (9, 81, 1),
       (2, 81, 2),
       (1, 81, 3),
       (82, 82, 0),
       (9, 82, 1),
       (2, 82, 2),
       (1, 82, 3);

-- H열 좌석 (id: 83~92, row: 10)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (83, 83, 0),
       (10, 83, 1),
       (2, 83, 2),
       (1, 83, 3),
       (84, 84, 0),
       (10, 84, 1),
       (2, 84, 2),
       (1, 84, 3),
       (85, 85, 0),
       (10, 85, 1),
       (2, 85, 2),
       (1, 85, 3),
       (86, 86, 0),
       (10, 86, 1),
       (2, 86, 2),
       (1, 86, 3),
       (87, 87, 0),
       (10, 87, 1),
       (2, 87, 2),
       (1, 87, 3),
       (88, 88, 0),
       (10, 88, 1),
       (2, 88, 2),
       (1, 88, 3),
       (89, 89, 0),
       (10, 89, 1),
       (2, 89, 2),
       (1, 89, 3),
       (90, 90, 0),
       (10, 90, 1),
       (2, 90, 2),
       (1, 90, 3),
       (91, 91, 0),
       (10, 91, 1),
       (2, 91, 2),
       (1, 91, 3),
       (92, 92, 0),
       (10, 92, 1),
       (2, 92, 2),
       (1, 92, 3);

-- I열 좌석 (id: 93~102, row: 11)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (93, 93, 0),
       (11, 93, 1),
       (2, 93, 2),
       (1, 93, 3),
       (94, 94, 0),
       (11, 94, 1),
       (2, 94, 2),
       (1, 94, 3),
       (95, 95, 0),
       (11, 95, 1),
       (2, 95, 2),
       (1, 95, 3),
       (96, 96, 0),
       (11, 96, 1),
       (2, 96, 2),
       (1, 96, 3),
       (97, 97, 0),
       (11, 97, 1),
       (2, 97, 2),
       (1, 97, 3),
       (98, 98, 0),
       (11, 98, 1),
       (2, 98, 2),
       (1, 98, 3),
       (99, 99, 0),
       (11, 99, 1),
       (2, 99, 2),
       (1, 99, 3),
       (100, 100, 0),
       (11, 100, 1),
       (2, 100, 2),
       (1, 100, 3),
       (101, 101, 0),
       (11, 101, 1),
       (2, 101, 2),
       (1, 101, 3),
       (102, 102, 0),
       (11, 102, 1),
       (2, 102, 2),
       (1, 102, 3);

-- J열 좌석 (id: 103~112, row: 12)
INSERT INTO resource_closure (ancestor_id, descendant_id, depth)
VALUES (103, 103, 0),
       (12, 103, 1),
       (2, 103, 2),
       (1, 103, 3),
       (104, 104, 0),
       (12, 104, 1),
       (2, 104, 2),
       (1, 104, 3),
       (105, 105, 0),
       (12, 105, 1),
       (2, 105, 2),
       (1, 105, 3),
       (106, 106, 0),
       (12, 106, 1),
       (2, 106, 2),
       (1, 106, 3),
       (107, 107, 0),
       (12, 107, 1),
       (2, 107, 2),
       (1, 107, 3),
       (108, 108, 0),
       (12, 108, 1),
       (2, 108, 2),
       (1, 108, 3),
       (109, 109, 0),
       (12, 109, 1),
       (2, 109, 2),
       (1, 109, 3),
       (110, 110, 0),
       (12, 110, 1),
       (2, 110, 2),
       (1, 110, 3),
       (111, 111, 0),
       (12, 111, 1),
       (2, 111, 2),
       (1, 111, 3),
       (112, 112, 0),
       (12, 112, 1),
       (2, 112, 2),
       (1, 112, 3);

-- ------------------------------------------------------------
-- 4. 기본 요금 (resource_rates) - 좌석별 등급 기반
-- ------------------------------------------------------------
-- VIP: 150,000원 (A,B열: 13~32)
-- R: 120,000원 (C,D열: 33~52)
-- S: 100,000원 (E,F,G열: 53~82)
-- A: 80,000원 (H,I,J열: 83~112)

-- VIP 좌석 요금 (A열: 13~22, B열: 23~32)
INSERT INTO resource_rates (resource_id, rate_type, amount, currency, priority, reason)
VALUES (13, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (14, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (15, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (16, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (17, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (18, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (19, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (20, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (21, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (22, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (23, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (24, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (25, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (26, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (27, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (28, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (29, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (30, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (31, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금'),
       (32, 'BASE', 150000, 'KRW', 0, 'VIP석 기본 요금');

-- R석 요금 (C열: 33~42, D열: 43~52)
INSERT INTO resource_rates (resource_id, rate_type, amount, currency, priority, reason)
VALUES (33, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (34, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (35, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (36, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (37, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (38, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (39, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (40, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (41, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (42, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (43, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (44, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (45, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (46, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (47, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (48, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (49, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (50, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (51, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금'),
       (52, 'BASE', 120000, 'KRW', 0, 'R석 기본 요금');

-- S석 요금 (E열: 53~62, F열: 63~72, G열: 73~82)
INSERT INTO resource_rates (resource_id, rate_type, amount, currency, priority, reason)
VALUES (53, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (54, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (55, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (56, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (57, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (58, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (59, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (60, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (61, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (62, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (63, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (64, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (65, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (66, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (67, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (68, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (69, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (70, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (71, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (72, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (73, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (74, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (75, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (76, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (77, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (78, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (79, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (80, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (81, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금'),
       (82, 'BASE', 100000, 'KRW', 0, 'S석 기본 요금');

-- A석 요금 (H열: 83~92, I열: 93~102, J열: 103~112)
INSERT INTO resource_rates (resource_id, rate_type, amount, currency, priority, reason)
VALUES (83, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (84, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (85, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (86, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (87, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (88, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (89, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (90, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (91, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (92, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (93, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (94, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (95, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (96, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (97, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (98, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (99, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (100, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (101, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (102, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (103, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (104, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (105, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (106, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (107, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (108, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (109, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (110, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (111, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금'),
       (112, 'BASE', 80000, 'KRW', 0, 'A석 기본 요금');
