-- 社区点赞与一级评论；互动对象仍复用 tm_travel_note。
CREATE TABLE IF NOT EXISTS tm_travel_note_like (
  travel_note_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (travel_note_id, user_id),
  INDEX idx_note_like_user (user_id),
  CONSTRAINT fk_note_like_note FOREIGN KEY (travel_note_id) REFERENCES tm_travel_note (id),
  CONSTRAINT fk_note_like_user FOREIGN KEY (user_id) REFERENCES tm_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tm_travel_note_comment (
  id BIGINT PRIMARY KEY,
  travel_note_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_note_comment_note (travel_note_id, deleted, create_time),
  INDEX idx_note_comment_user (user_id),
  CONSTRAINT fk_note_comment_note FOREIGN KEY (travel_note_id) REFERENCES tm_travel_note (id),
  CONSTRAINT fk_note_comment_user FOREIGN KEY (user_id) REFERENCES tm_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
