ALTER TABLE tm_trip_memory
  ADD COLUMN index_status VARCHAR(32) NOT NULL DEFAULT 'pending' AFTER generation_status,
  ADD COLUMN indexed_at DATETIME NULL AFTER index_status;
