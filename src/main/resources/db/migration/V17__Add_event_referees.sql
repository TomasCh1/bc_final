-- Referees per match (up to 3 per event), name + optional grade
CREATE TABLE event_referees (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    grade VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE (event_id, sort_order),
    CHECK (sort_order >= 0 AND sort_order < 3)
);

CREATE INDEX idx_event_referees_event_id ON event_referees(event_id);
