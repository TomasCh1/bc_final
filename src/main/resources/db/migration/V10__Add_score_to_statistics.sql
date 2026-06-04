-- Add match score columns to statistics (our team score, opponent score). Match-only.
ALTER TABLE statistics DROP COLUMN IF  EXISTS our_score INTEGER;
ALTER TABLE statistics DROP COLUMN IF  EXISTS opponent_score INTEGER;
