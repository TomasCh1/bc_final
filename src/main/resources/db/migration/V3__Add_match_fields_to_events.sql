-- Add isHomeMatch and matchWon columns to events table
-- isHomeMatch: true if match is at home (location in Trnava), false if away
-- matchWon: true if match was won, false if lost, NULL if not yet played or not applicable

ALTER TABLE events 
ADD COLUMN IF NOT EXISTS is_home_match BOOLEAN,
ADD COLUMN IF NOT EXISTS match_won BOOLEAN;

-- Update existing MATCH events to set is_home_match based on location
UPDATE events 
SET is_home_match = CASE 
    WHEN location IS NOT NULL AND LOWER(location) LIKE '%trnava%' THEN TRUE
    WHEN location IS NOT NULL AND LOWER(location) NOT LIKE '%trnava%' THEN FALSE
    ELSE NULL
END
WHERE type = 'MATCH';

