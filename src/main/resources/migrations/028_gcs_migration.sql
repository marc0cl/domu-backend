-- Migration: GCS storage support & public display names
-- Date: 2026-02-07

-- Add public display name for chat / community visibility
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(100) NULL;
