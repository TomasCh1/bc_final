-- Users table with RBAC support
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'TRAINER', 'PLAYER', 'GUEST')),
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT FALSE
);

-- Categories table (U13, U15, etc.)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    season VARCHAR(20) NOT NULL,
    coach_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coach_id) REFERENCES users(id)
);

-- Events table (training/match types)
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('TRAINING', 'MATCH')),
    date DATE NOT NULL,
    time TIME,
    location VARCHAR(255),
    category_id BIGINT,
    opponent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Attendance table
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    trainer_id BIGINT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES users(id),
    FOREIGN KEY (trainer_id) REFERENCES users(id),
    UNIQUE(event_id, player_id)
);

-- Statistics table with JSON field for stat values
CREATE TABLE statistics (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    stat_type VARCHAR(20) NOT NULL CHECK (stat_type IN ('GAME', 'TRAINING')),
    values JSONB NOT NULL,
    opponent VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES users(id),
    CHECK ((stat_type = 'GAME' AND opponent IS NOT NULL) OR (stat_type = 'TRAINING'))
);

-- Training statistics table for personal training stats
CREATE TABLE training_statistics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exercise_name VARCHAR(64) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('POSLNOVANIE', 'BEH', 'STRELBA')),
    weight DECIMAL(10, 2),
    repetitions INTEGER,
    time VARCHAR(20),
    distance DECIMAL(10, 2),
    shots_attempted INTEGER,
    shots_made INTEGER,
    success_rate DECIMAL(5, 2),
    court_spot VARCHAR(255),
    note TEXT,
    workout_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Messages table (for Telegram integration prep)
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT,
    group_id BIGINT,
    content TEXT NOT NULL,
    telegram_message_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    CHECK ((receiver_id IS NOT NULL AND group_id IS NULL) OR (receiver_id IS NULL AND group_id IS NOT NULL))
);

-- Exports table
CREATE TABLE exports (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL CHECK (format IN ('PDF', 'XML', 'XLSM', 'EXCEL')),
    file_path VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Add foreign key for users.category_id
ALTER TABLE users ADD CONSTRAINT fk_users_category 
    FOREIGN KEY (category_id) REFERENCES categories(id);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_category ON users(category_id);
CREATE INDEX idx_events_date ON events(date);
CREATE INDEX idx_events_category ON events(category_id);
CREATE INDEX idx_events_type ON events(type);
CREATE INDEX idx_attendance_event ON attendance(event_id);
CREATE INDEX idx_attendance_player ON attendance(player_id);
CREATE INDEX idx_statistics_event ON statistics(event_id);
CREATE INDEX idx_statistics_player ON statistics(player_id);
CREATE INDEX idx_statistics_type ON statistics(stat_type);
CREATE INDEX idx_training_statistics_user ON training_statistics(user_id);
CREATE INDEX idx_training_statistics_category ON training_statistics(category);
CREATE INDEX idx_training_statistics_created ON training_statistics(created_at);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_receiver ON messages(receiver_id);

