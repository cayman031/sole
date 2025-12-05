-- Initial schema aligned with plan.md (User, Region, RunningCrew, CrewMember)

CREATE TABLE regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    CONSTRAINT UK_region_city_district UNIQUE (city, district)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    region_id BIGINT NULL,
    preferred_level VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT UK_user_email UNIQUE (email),
    CONSTRAINT FK_users_region FOREIGN KEY (region_id) REFERENCES regions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE running_crews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT NULL,
    host_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    meeting_time DATETIME NOT NULL,
    place VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    max_participants INT NOT NULL,
    level VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT FK_running_crews_host FOREIGN KEY (host_id) REFERENCES users (id),
    CONSTRAINT FK_running_crews_region FOREIGN KEY (region_id) REFERENCES regions (id),
    CONSTRAINT CK_running_crews_max_participants CHECK (max_participants > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crew_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UK_crew_member UNIQUE (crew_id, user_id),
    CONSTRAINT FK_crew_members_crew FOREIGN KEY (crew_id) REFERENCES running_crews (id),
    CONSTRAINT FK_crew_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_user_region ON users (region_id);
CREATE INDEX IDX_crew_region_meeting ON running_crews (region_id, meeting_time);
CREATE INDEX IDX_crew_lat_lng ON running_crews (latitude, longitude);
CREATE INDEX IDX_member_user ON crew_members (user_id);
