ALTER TABLE event_nominated_players
    ADD COLUMN jersey_number INTEGER;

ALTER TABLE event_nominated_players
    ADD CONSTRAINT chk_event_nominated_players_jersey_positive
    CHECK (jersey_number IS NULL OR jersey_number > 0);

CREATE UNIQUE INDEX ux_event_nominated_players_event_jersey
    ON event_nominated_players(event_id, jersey_number)
    WHERE jersey_number IS NOT NULL;
