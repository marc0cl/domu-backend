-- Add privacy photo support for neighbors
ALTER TABLE users ADD COLUMN privacy_avatar_box_id VARCHAR(100) NULL;
