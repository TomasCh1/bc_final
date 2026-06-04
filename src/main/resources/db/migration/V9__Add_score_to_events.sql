-- Add match score columns to events (our team score, opponent score)
ALTER TABLE events ADD COLUMN IF NOT EXISTS our_score INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS opponent_score INTEGER;
