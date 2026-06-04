-- Match nominations per event (coach-selected players for a match)
CREATE TABLE event_nominated_players (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES users(id),
    UNIQUE (event_id, player_id)
);

CREATE INDEX idx_event_nominated_players_event_id
    ON event_nominated_players(event_id);

CREATE INDEX idx_event_nominated_players_player_id
    ON event_nominated_players(player_id);
