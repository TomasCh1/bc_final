-- Allow coach/admin to mark a training as cancelled (display with strikethrough + "Zrušené")
ALTER TABLE events
ADD COLUMN IF NOT EXISTS cancelled BOOLEAN DEFAULT FALSE;
