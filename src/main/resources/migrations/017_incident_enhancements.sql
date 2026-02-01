-- Enhancements for incidents: task assignments
-- building_id already exists in the table according to current DDL

ALTER TABLE incidents 
  ADD COLUMN assigned_to_user_id BIGINT NULL AFTER status;

-- Add foreign key for the assignee
ALTER TABLE incidents 
  ADD CONSTRAINT fk_incidents_assigned_to FOREIGN KEY (assigned_to_user_id) REFERENCES users(id);

-- Index for better performance
CREATE INDEX idx_incidents_assigned_to ON incidents (assigned_to_user_id);