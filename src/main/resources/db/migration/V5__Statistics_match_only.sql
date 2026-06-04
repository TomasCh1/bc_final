-- Statistics table: match-only. Training data lives in training_statistics.
-- Remove any TRAINING rows, then restrict statistics to GAME with opponent required.

DELETE FROM statistics WHERE stat_type = 'TRAINING';

ALTER TABLE statistics DROP CONSTRAINT IF EXISTS statistics_stat_type_check;
ALTER TABLE statistics DROP CONSTRAINT IF EXISTS statistics_check;

ALTER TABLE statistics ADD CONSTRAINT statistics_match_only
    CHECK (stat_type = 'GAME' AND opponent IS NOT NULL);

ALTER TABLE statistics ALTER COLUMN opponent SET NOT NULL;
