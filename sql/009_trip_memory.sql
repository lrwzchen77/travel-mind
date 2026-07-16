-- 私有旅行记忆册：业务数据留在 MySQL，AI 只消费 Java 校验后的项目。
CREATE TABLE IF NOT EXISTS tm_trip_memory (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  trip_plan_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  destination_city VARCHAR(128) NOT NULL,
  summary VARCHAR(1000) NULL,
  cover_image VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  visibility VARCHAR(16) NOT NULL DEFAULT 'private',
  generation_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_trip_memory_trip (trip_plan_id),
  INDEX idx_trip_memory_user (user_id, update_time),
  CONSTRAINT fk_trip_memory_user FOREIGN KEY (user_id) REFERENCES tm_user (id),
  CONSTRAINT fk_trip_memory_trip FOREIGN KEY (trip_plan_id) REFERENCES tm_trip_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_trip_memory_item (
  id BIGINT PRIMARY KEY,
  memory_id BIGINT NOT NULL,
  item_type VARCHAR(32) NOT NULL,
  source_type VARCHAR(32) NULL,
  source_id BIGINT NULL,
  source_url VARCHAR(512) NULL,
  taken_at DATETIME NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  city VARCHAR(128) NULL,
  place_name VARCHAR(255) NULL,
  content TEXT NULL,
  ai_caption VARCHAR(2000) NULL,
  ai_tags JSON NULL,
  confidence DECIMAL(5,4) NULL,
  day_index INT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ready',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_memory_item_source (memory_id, source_type, source_id),
  INDEX idx_memory_item_timeline (memory_id, day_index, taken_at, sort_order),
  CONSTRAINT fk_memory_item_memory FOREIGN KEY (memory_id) REFERENCES tm_trip_memory (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_trip_memory_generation (
  id BIGINT PRIMARY KEY,
  memory_id BIGINT NOT NULL,
  generation_type VARCHAR(32) NOT NULL,
  content LONGTEXT NOT NULL,
  evidence_json JSON NOT NULL,
  version INT NOT NULL,
  accepted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_memory_generation_version (memory_id, generation_type, version),
  INDEX idx_memory_generation_memory (memory_id, create_time),
  CONSTRAINT fk_memory_generation_memory FOREIGN KEY (memory_id) REFERENCES tm_trip_memory (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
