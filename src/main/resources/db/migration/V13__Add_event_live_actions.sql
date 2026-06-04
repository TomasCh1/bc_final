CREATE TABLE event_live_actions (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    period INTEGER NOT NULL,
    clock_seconds INTEGER NOT NULL,
    team_side VARCHAR(20) NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    our_player_id BIGINT,
    opponent_jersey_number INTEGER,
    points INTEGER,
    on_court_player_ids_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (our_player_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT event_live_actions_action_type_check CHECK (action_type IN ('SCORE', 'FOUL', 'SUB_IN', 'SUB_OUT')),
    CONSTRAINT chk_event_live_actions_period_positive CHECK (period > 0),
    CONSTRAINT chk_event_live_actions_clock_non_negative CHECK (clock_seconds >= 0),
    CONSTRAINT chk_event_live_actions_points_valid CHECK (points IS NULL OR points IN (0, 1, 2, 3)),
    CONSTRAINT chk_event_live_actions_opponent_jersey_positive CHECK (opponent_jersey_number IS NULL OR opponent_jersey_number > 0)
);

CREATE INDEX idx_event_live_actions_event_id
    ON event_live_actions(event_id);

CREATE INDEX idx_event_live_actions_event_created
    ON event_live_actions(event_id, created_at, id);

CREATE INDEX idx_event_live_actions_event_opp_jersey
    ON event_live_actions(event_id, opponent_jersey_number);
