CREATE TABLE event_opponent_players (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    jersey_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT chk_event_opponent_players_jersey_positive CHECK (jersey_number > 0),
    CONSTRAINT ux_event_opponent_players_event_jersey UNIQUE (event_id, jersey_number)
);

CREATE INDEX idx_event_opponent_players_event_id
    ON event_opponent_players(event_id);
