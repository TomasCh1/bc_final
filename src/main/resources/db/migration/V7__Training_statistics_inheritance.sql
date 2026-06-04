-- Step 1: Create child tables
CREATE TABLE poslnovanie_stats (
    id BIGINT PRIMARY KEY REFERENCES training_statistics(id) ON DELETE CASCADE,
    weight DECIMAL(10, 2),
    repetitions INTEGER
);

CREATE TABLE beh_stats (
    id BIGINT PRIMARY KEY REFERENCES training_statistics(id) ON DELETE CASCADE,
    time VARCHAR(20),
    distance DECIMAL(10, 2)
);

CREATE TABLE strelba_stats (
    id BIGINT PRIMARY KEY REFERENCES training_statistics(id) ON DELETE CASCADE,
    shots_attempted INTEGER,
    shots_made INTEGER,
    success_rate DECIMAL(5, 2),
    court_spot VARCHAR(255)
);

-- Step 2: Migrate data from training_statistics to child tables
INSERT INTO poslnovanie_stats (id, weight, repetitions)
SELECT id, weight, repetitions FROM training_statistics WHERE category = 'POSLNOVANIE';

INSERT INTO beh_stats (id, time, distance)
SELECT id, time, distance FROM training_statistics WHERE category = 'BEH';

INSERT INTO strelba_stats (id, shots_attempted, shots_made, success_rate, court_spot)
SELECT id, shots_attempted, shots_made, success_rate, court_spot
FROM training_statistics WHERE category = 'STRELBA';

-- Step 3: Drop category-specific columns from base table
ALTER TABLE training_statistics DROP COLUMN IF EXISTS weight;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS repetitions;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS time;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS distance;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS shots_attempted;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS shots_made;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS success_rate;
ALTER TABLE training_statistics DROP COLUMN IF EXISTS court_spot;
