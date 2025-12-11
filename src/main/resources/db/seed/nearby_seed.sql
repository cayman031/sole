-- Seed data for nearby crew performance baseline

-- Region
INSERT INTO regions (id, city, district)
VALUES (100, '서울시', '중구')
ON DUPLICATE KEY UPDATE city = VALUES(city), district = VALUES(district);

-- Users (passwords are dummy placeholders; replace with real BCrypt if auth is needed)
INSERT INTO users (id, email, password, nickname, region_id, preferred_level, created_at, updated_at) VALUES
    (1000, 'user1@test.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5NRGIKGPLp5a0wz1AIP9h5d/.p/Di', 'user1', 100, 'BEGINNER', NOW(), NOW()), -- password
    (1001, 'user2@test.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5NRGIKGPLp5a0wz1AIP9h5d/.p/Di', 'user2', 100, 'BEGINNER', NOW(), NOW()),
    (1002, 'user3@test.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5NRGIKGPLp5a0wz1AIP9h5d/.p/Di', 'user3', 100, 'INTERMEDIATE', NOW(), NOW()),
    (1003, 'user4@test.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5NRGIKGPLp5a0wz1AIP9h5d/.p/Di', 'user4', 100, 'ADVANCED', NOW(), NOW()),
    (1004, 'user5@test.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5NRGIKGPLp5a0wz1AIP9h5d/.p/Di', 'user5', 100, 'BEGINNER', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    password = VALUES(password),
    preferred_level = VALUES(preferred_level),
    region_id = VALUES(region_id);

-- Running crews: 500 rows around Seoul Station (approx. ±4.4km)
DELETE FROM running_crews WHERE id BETWEEN 2000 AND 2499;
INSERT INTO running_crews
    (id, title, description, host_id, region_id, meeting_time, place, latitude, longitude, max_participants, level, created_at)
SELECT
    2000 + (s1 + s2 + s3) AS id,
    CONCAT('크루', (s1 + s2 + s3)) AS title,
    'seeded crew' AS description,
    1000 + ((s1 + s2 + s3) % 5) AS host_id,
    100 AS region_id,
    DATE_ADD(NOW(), INTERVAL ((s1 + s2 + s3) % 30) DAY) AS meeting_time,
    '서울역 인근' AS place,
    37.55 + (RAND(s1 + s2 + s3) - 0.5) * 0.08 AS latitude,
    126.97 + (RAND(s1 + s2 + s3 + 1) - 0.5) * 0.08 AS longitude,
    10 + ((s1 + s2 + s3) % 10) AS max_participants,
    CASE ((s1 + s2 + s3) % 3)
        WHEN 0 THEN 'BEGINNER'
        WHEN 1 THEN 'INTERMEDIATE'
        ELSE 'ADVANCED'
    END AS level,
    NOW() AS created_at
FROM (
    SELECT 0 AS s1 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) t1
CROSS JOIN (
    SELECT 0 AS s2 UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40
    UNION ALL SELECT 50 UNION ALL SELECT 60 UNION ALL SELECT 70 UNION ALL SELECT 80 UNION ALL SELECT 90
) t2
CROSS JOIN (
    SELECT 0 AS s3 UNION ALL SELECT 100 UNION ALL SELECT 200 UNION ALL SELECT 300 UNION ALL SELECT 400
) t3;
-- t1(10) * t2(10) * t3(5) = 500 rows (ids 2000~2499)
