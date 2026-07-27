ALTER TABLE tm_travel_note
  ADD COLUMN review_reason VARCHAR(500) NULL AFTER status,
  ADD COLUMN reviewed_by BIGINT NULL AFTER review_reason,
  ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by;
