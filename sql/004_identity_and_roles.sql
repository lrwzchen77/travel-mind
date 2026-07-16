USE travelmind;

CREATE TABLE IF NOT EXISTS tm_identity_account (
  user_id BIGINT PRIMARY KEY,
  password_hash VARCHAR(100) NOT NULL,
  role_code VARCHAR(32) NOT NULL DEFAULT 'user',
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_identity_role (role_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Development accounts are created by DevIdentityBootstrap with BCrypt hashes.
-- Production must provision accounts with deployment-specific passwords.
