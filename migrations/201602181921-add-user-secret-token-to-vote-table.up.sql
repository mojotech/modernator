ALTER TABLE votes ADD user_secret_token text;

ALTER TABLE votes ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE items ALTER COLUMN added_by DROP NOT NULL;
