-- Allow users to hide chat rooms from their view (soft-delete per participant)
ALTER TABLE chat_participant ADD COLUMN hidden_at TIMESTAMP NULL DEFAULT NULL;
