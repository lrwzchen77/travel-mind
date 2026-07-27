-- C 端旅行灵感社区、灵感包与 AI 伴游会话。
-- 帖子复用既有 tm_travel_note；公开内容由 status=1 控制，避免另建内容体系。
ALTER TABLE tm_travel_note
  ADD COLUMN topic VARCHAR(32) NULL,
  ADD COLUMN cover_image VARCHAR(500) NULL,
  ADD COLUMN tags VARCHAR(255) NULL;

CREATE TABLE IF NOT EXISTS tm_inspiration_item (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  travel_note_id BIGINT NOT NULL,
  intent VARCHAR(32) NOT NULL DEFAULT 'reference',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_inspiration_user_note (user_id, travel_note_id),
  INDEX idx_inspiration_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_ai_conversation (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  scene VARCHAR(32) NOT NULL DEFAULT 'explore',
  trip_plan_id BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_ai_conversation_user (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_ai_message (
  id BIGINT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  metadata_json JSON NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ai_message_conversation (conversation_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE tm_travel_note
SET topic = COALESCE(topic, 'route')
WHERE visibility = 'public' AND deleted = 0;
