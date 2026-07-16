-- 用户为自己的行程记录实际花费；不承载支付、订单或报销流程。
CREATE TABLE IF NOT EXISTS tm_trip_expense (
  id BIGINT PRIMARY KEY,
  trip_plan_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  spent_on DATE NOT NULL,
  note VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_trip_expense_plan (trip_plan_id, spent_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
