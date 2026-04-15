-- Add cached summary payload to interviews so we only call Claude once per interview
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS summary_payload JSONB;
