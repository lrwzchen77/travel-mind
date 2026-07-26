USE travelmind;

-- 统一景点/住宿/餐饮主数据：高德原始 POI 与管理员手动补充均存入 tm_map_poi。
SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'tm_map_poi' AND column_name = 'city_id');
SET @ddl = IF(@column_exists = 0, 'ALTER TABLE tm_map_poi ADD COLUMN city_id BIGINT NULL AFTER city', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'tm_map_poi' AND column_name = 'create_time');
SET @ddl = IF(@column_exists = 0,
  'ALTER TABLE tm_map_poi ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER status', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'tm_map_poi' AND column_name = 'update_time');
SET @ddl = IF(@column_exists = 0,
  'ALTER TABLE tm_map_poi ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE tm_map_poi p
JOIN tm_city c ON c.name = p.city AND c.deleted = 0
SET p.city_id = c.id
WHERE p.city_id IS NULL;

SET @city_kind_index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tm_map_poi' AND index_name = 'idx_map_poi_city_id_kind'
);
SET @city_kind_index_sql = IF(@city_kind_index_exists = 0,
  'CREATE INDEX idx_map_poi_city_id_kind ON tm_map_poi (city_id, kind, status, deleted)', 'SELECT 1');
PREPARE city_kind_index_stmt FROM @city_kind_index_sql;
EXECUTE city_kind_index_stmt;
DEALLOCATE PREPARE city_kind_index_stmt;
