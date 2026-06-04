-- Replace statistics.values JSONB with dedicated columns for all stats

ALTER TABLE statistics
    ADD COLUMN IF NOT EXISTS min DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS two_p_made INTEGER,
    ADD COLUMN IF NOT EXISTS two_p_attempts INTEGER,
    ADD COLUMN IF NOT EXISTS three_p_made INTEGER,
    ADD COLUMN IF NOT EXISTS three_p_attempts INTEGER,
    ADD COLUMN IF NOT EXISTS fg_made INTEGER,
    ADD COLUMN IF NOT EXISTS fg_attempts INTEGER,
    ADD COLUMN IF NOT EXISTS ft_made INTEGER,
    ADD COLUMN IF NOT EXISTS ft_attempts INTEGER,
    ADD COLUMN IF NOT EXISTS pts INTEGER,
    ADD COLUMN IF NOT EXISTS fouls_plus INTEGER,
    ADD COLUMN IF NOT EXISTS fouls_minus INTEGER,
    ADD COLUMN IF NOT EXISTS off_reb INTEGER,
    ADD COLUMN IF NOT EXISTS def_reb INTEGER,
    ADD COLUMN IF NOT EXISTS stl INTEGER,
    ADD COLUMN IF NOT EXISTS ast INTEGER,
    ADD COLUMN IF NOT EXISTS blk INTEGER,
    ADD COLUMN IF NOT EXISTS turnovers INTEGER,
    ADD COLUMN IF NOT EXISTS stat_index DOUBLE PRECISION;

-- Migrate existing data from JSONB to columns (only when values column exists and has content)
UPDATE statistics
SET
    min = (values->>'MIN')::double precision,
    two_p_made = (values->>'2PÚ')::integer,
    two_p_attempts = (values->>'2PC')::integer,
    three_p_made = (values->>'3PÚ')::integer,
    three_p_attempts = (values->>'3PC')::integer,
    fg_made = (values->>'FGÚ')::integer,
    fg_attempts = (values->>'FGC')::integer,
    ft_made = (values->>'FTÚ')::integer,
    ft_attempts = (values->>'FTC')::integer,
    pts = (values->>'PTS')::integer,
    fouls_plus = (values->>'F+')::integer,
    fouls_minus = (values->>'F-')::integer,
    off_reb = (values->>'OFF')::integer,
    def_reb = (values->>'DEF')::integer,
    stl = (values->>'STL')::integer,
    ast = (values->>'AST')::integer,
    blk = (values->>'BLK')::integer,
    turnovers = (values->>'TO')::integer,
    stat_index = (values->>'INDEX')::double precision
WHERE values IS NOT NULL AND values::text != 'null';

-- Drop the JSONB column
ALTER TABLE statistics DROP COLUMN IF EXISTS values;
