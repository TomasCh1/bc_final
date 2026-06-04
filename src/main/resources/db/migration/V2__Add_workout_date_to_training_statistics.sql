-- Add workout_date column to training_statistics table
-- This allows users to record training stats for past workouts

ALTER TABLE training_statistics 
ADD COLUMN IF NOT EXISTS workout_date DATE NOT NULL DEFAULT CURRENT_DATE;

-- Update existing records to use created_at date as workout_date
UPDATE training_statistics 
SET workout_date = CAST(created_at AS DATE) 
WHERE workout_date IS NULL;
