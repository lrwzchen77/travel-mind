ALTER TABLE tm_identity_account
  ADD CONSTRAINT fk_identity_account_user
  FOREIGN KEY (user_id) REFERENCES tm_user (id);
