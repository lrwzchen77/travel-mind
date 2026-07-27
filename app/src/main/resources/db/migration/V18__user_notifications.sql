CREATE TABLE IF NOT EXISTS tm_notification (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(500) NOT NULL,
  target_url VARCHAR(255) NULL,
  read_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notification_user (user_id, read_at, create_time),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES tm_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
