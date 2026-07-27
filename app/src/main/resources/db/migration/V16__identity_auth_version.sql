ALTER TABLE tm_identity_account
  ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 AFTER status;
