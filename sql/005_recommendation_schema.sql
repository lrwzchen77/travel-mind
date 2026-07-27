USE travelmind;

CREATE TABLE IF NOT EXISTS tm_recommendation_log (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  recommend_type VARCHAR(32) NOT NULL COMMENT 'city/attraction/hotel/restaurant/trip',
  target_id BIGINT NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  score DECIMAL(5,4) NOT NULL COMMENT '推荐置信度 0-1',
  user_feedback VARCHAR(32) NULL COMMENT 'click/ignore/save/like',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_rec_user (user_id),
  INDEX idx_rec_type (recommend_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
