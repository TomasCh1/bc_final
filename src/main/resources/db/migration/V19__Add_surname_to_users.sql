-- Add surname column; split existing full name (given + surname) where possible
ALTER TABLE users ADD COLUMN surname VARCHAR(255);

UPDATE users SET
    surname = CASE
        WHEN trim(name) ~ '\s' THEN (regexp_match(trim(name), '(\S+)$'))[1]
        ELSE ''
    END,
    name = CASE
        WHEN trim(name) ~ '\s' THEN trim(regexp_replace(trim(name), '\s+\S+$', ''))
        ELSE trim(name)
    END;

ALTER TABLE users ALTER COLUMN surname SET DEFAULT '';
ALTER TABLE users ALTER COLUMN surname SET NOT NULL;
