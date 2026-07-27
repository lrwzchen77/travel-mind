USE travelmind;

CREATE TABLE IF NOT EXISTS tm_travel_journal (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  trip_plan_id BIGINT NULL COMMENT '关联的行程计划',
  title VARCHAR(128) NOT NULL,
  cover_image VARCHAR(512) NULL,
  destination_city VARCHAR(128) NULL,
  travel_days INT NOT NULL DEFAULT 1,
  summary TEXT NULL COMMENT '游记简介',
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  visibility VARCHAR(32) NOT NULL DEFAULT 'private',
  view_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_journal_user (user_id),
  INDEX idx_journal_trip (trip_plan_id),
  INDEX idx_journal_status (status, visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_journal_photo (
  id BIGINT PRIMARY KEY,
  journal_id BIGINT NOT NULL,
  photo_url VARCHAR(512) NOT NULL,
  caption VARCHAR(255) NULL COMMENT '照片说明',
  location VARCHAR(255) NULL COMMENT '拍摄地点',
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  day_index INT NULL COMMENT '第几天拍摄',
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_photo_journal (journal_id),
  INDEX idx_photo_day (journal_id, day_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_journal_location (
  id BIGINT PRIMARY KEY,
  journal_id BIGINT NOT NULL,
  place_name VARCHAR(128) NOT NULL,
  place_type VARCHAR(32) NULL COMMENT 'attraction/hotel/restaurant',
  address VARCHAR(255) NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  day_index INT NOT NULL DEFAULT 1,
  time_of_day VARCHAR(32) NULL COMMENT 'morning/afternoon/evening',
  description VARCHAR(1000) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_location_journal (journal_id, day_index, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
