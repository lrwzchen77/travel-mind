CREATE TABLE IF NOT EXISTS tm_trip_comfort_feedback (
  id BIGINT PRIMARY KEY,
  trip_plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  actual_label VARCHAR(16) NOT NULL,
  note VARCHAR(500) NULL,
  prediction_json JSON NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_comfort_feedback_trip_user (trip_plan_id, user_id),
  INDEX idx_comfort_feedback_label (actual_label),
  INDEX idx_comfort_feedback_updated (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
