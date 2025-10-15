-- ============================================================================
-- Smart Packing Assistant - PostgreSQL Schema
-- ============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Tabelle 1: sessions
-- Zweck: Session-Management ohne User-Authentication
-- ============================================================================
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,

    CONSTRAINT sessions_token_length CHECK (LENGTH(session_token) >= 32)
);

CREATE INDEX idx_sessions_token ON sessions(session_token);
CREATE INDEX idx_sessions_active ON sessions(is_active, last_activity);

-- ============================================================================
-- Tabelle 2: packing_lists
-- Zweck: Haupttabelle für generierte Packlisten
-- ============================================================================
CREATE TABLE packing_lists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,

    -- Request Parameters
    destination VARCHAR(255) NOT NULL,
    duration_days INT NOT NULL CHECK (duration_days > 0 AND duration_days <= 365),
    travel_type VARCHAR(50) NOT NULL,  -- BUSINESS, VACATION, BACKPACKING
    travel_date DATE,
    season VARCHAR(20) NOT NULL,       -- SPRING, SUMMER, FALL, WINTER

    -- AI Generated Content (JSONB für Flexibilität)
    items_json JSONB NOT NULL,
    -- Struktur:
    -- {
    --   "categories": {
    --     "clothing": [{"item": "T-Shirt", "quantity": 3, "reason": "..."}],
    --     "tech": [...],
    --     "hygiene": [...],
    --     "documents": [...],
    --     "other": [...]
    --   }
    -- }

    weather_info JSONB,
    -- Struktur:
    -- {
    --   "temperature_range": {"min": 15, "max": 25},
    --   "conditions": "Sunny with occasional rain",
    --   "humidity": "60%"
    -- }

    culture_tips TEXT[],
    special_notes TEXT,

    -- Metadata
    ai_model VARCHAR(100) DEFAULT 'gpt-4',
    generation_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT packing_lists_travel_type_check
        CHECK (travel_type IN ('BUSINESS', 'VACATION', 'BACKPACKING')),
    CONSTRAINT packing_lists_season_check
        CHECK (season IN ('SPRING', 'SUMMER', 'FALL', 'WINTER'))
);

CREATE INDEX idx_packing_lists_session ON packing_lists(session_id);
CREATE INDEX idx_packing_lists_destination ON packing_lists(destination);
CREATE INDEX idx_packing_lists_created ON packing_lists(created_at DESC);

-- GIN Index für JSONB Suche (Performance)
CREATE INDEX idx_packing_lists_items_json ON packing_lists USING GIN (items_json);

-- ============================================================================
-- Tabelle 3: chat_messages
-- Zweck: Chat-Historie für Nachfragen zu Packlisten
-- ============================================================================
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    packing_list_id UUID NOT NULL REFERENCES packing_lists(id) ON DELETE CASCADE,

    -- Message Content
    role VARCHAR(20) NOT NULL,  -- USER, ASSISTANT
    content TEXT NOT NULL,

    -- Metadata
    ai_model VARCHAR(100),
    tokens_used INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chat_messages_role_check
        CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_messages_packing_list ON chat_messages(packing_list_id, created_at);

-- ============================================================================
-- Tabelle 4: dummy_weather_data
-- Zweck: Vordefinierte Wetterdaten für Dummy-Simulation
-- ============================================================================
CREATE TABLE dummy_weather_data (
    id SERIAL PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    season VARCHAR(20) NOT NULL,

    -- Weather Details
    temp_min INT NOT NULL,
    temp_max INT NOT NULL,
    conditions VARCHAR(255) NOT NULL,
    humidity VARCHAR(50),
    precipitation_chance INT CHECK (precipitation_chance >= 0 AND precipitation_chance <= 100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT dummy_weather_season_check
        CHECK (season IN ('SPRING', 'SUMMER', 'FALL', 'WINTER')),
    UNIQUE (location, season)
);

CREATE INDEX idx_dummy_weather_location ON dummy_weather_data(location, season);

-- ============================================================================
-- Tabelle 5: dummy_culture_tips
-- Zweck: Vordefinierte Kultur-Tipps für verschiedene Destinationen
-- ============================================================================
CREATE TABLE dummy_culture_tips (
    id SERIAL PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL, -- DRESS_CODE, CUSTOMS, LANGUAGE, SAFETY
    tip TEXT NOT NULL,
    importance VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT dummy_culture_importance_check
        CHECK (importance IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX idx_dummy_culture_location ON dummy_culture_tips(location);

-- ============================================================================
-- Views für einfachere Abfragen
-- ============================================================================

-- View: Aktuelle Sessions mit Anzahl Packlisten
CREATE VIEW active_sessions_summary AS
SELECT
    s.id,
    s.session_token,
    s.created_at,
    s.last_activity,
    COUNT(pl.id) as packing_lists_count
FROM sessions s
LEFT JOIN packing_lists pl ON s.id = pl.session_id
WHERE s.is_active = TRUE
GROUP BY s.id, s.session_token, s.created_at, s.last_activity;

-- View: Packlisten mit Chat-Anzahl
CREATE VIEW packing_lists_with_chat_count AS
SELECT
    pl.id,
    pl.destination,
    pl.travel_type,
    pl.duration_days,
    pl.created_at,
    COUNT(cm.id) as chat_messages_count
FROM packing_lists pl
LEFT JOIN chat_messages cm ON pl.id = cm.packing_list_id
GROUP BY pl.id, pl.destination, pl.travel_type, pl.duration_days, pl.created_at;

-- ============================================================================
-- Seed Data: Dummy-Wetterdaten
-- ============================================================================
INSERT INTO dummy_weather_data (location, season, temp_min, temp_max, conditions, humidity, precipitation_chance) VALUES
('Island', 'SPRING', 2, 8, 'Cloudy with frequent rain and wind', '75%', 70),
('Island', 'SUMMER', 10, 15, 'Mild with occasional rain', '65%', 50),
('Island', 'FALL', 3, 9, 'Windy with rain', '70%', 65),
('Island', 'WINTER', -3, 3, 'Cold with snow and ice', '80%', 80),

('Dubai', 'SPRING', 22, 35, 'Hot and dry', '45%', 5),
('Dubai', 'SUMMER', 30, 45, 'Extremely hot', '50%', 2),
('Dubai', 'FALL', 25, 38, 'Hot and sunny', '48%', 3),
('Dubai', 'WINTER', 15, 28, 'Warm and pleasant', '55%', 10),

('Tokyo', 'SPRING', 12, 20, 'Mild with cherry blossoms', '60%', 40),
('Tokyo', 'SUMMER', 24, 32, 'Hot and humid', '75%', 60),
('Tokyo', 'FALL', 15, 23, 'Cool and comfortable', '65%', 45),
('Tokyo', 'WINTER', 3, 12, 'Cold and dry', '50%', 20);

-- ============================================================================
-- Seed Data: Dummy-Kultur-Tipps
-- ============================================================================
INSERT INTO dummy_culture_tips (location, category, tip, importance) VALUES
('Dubai', 'DRESS_CODE', 'Schultern und Knie sollten bedeckt sein, besonders in öffentlichen Bereichen', 'HIGH'),
('Dubai', 'CUSTOMS', 'Alkohol ist nur in lizenzierten Hotels erlaubt', 'HIGH'),
('Dubai', 'CUSTOMS', 'Öffentliche Zuneigungsbekundungen sind nicht erlaubt', 'MEDIUM'),

('Island', 'SAFETY', 'Wetter kann sich schnell ändern - immer mehrere Schichten einpacken', 'HIGH'),
('Island', 'CUSTOMS', 'Schuhe vor dem Betreten von Häusern ausziehen', 'MEDIUM'),
('Island', 'LANGUAGE', 'Englisch wird fast überall gesprochen', 'LOW'),

('Tokyo', 'CUSTOMS', 'Trinkgeld ist unüblich und kann als beleidigend empfunden werden', 'HIGH'),
('Tokyo', 'DRESS_CODE', 'Saubere Schuhe sind wichtig - oft werden Schuhe ausgezogen', 'MEDIUM'),
('Tokyo', 'CUSTOMS', 'Leise sprechen in öffentlichen Verkehrsmitteln', 'MEDIUM');

-- ============================================================================
-- Hilfsfunktionen
-- ============================================================================

-- Funktion: Session aufräumen (inactive nach 24h)
CREATE OR REPLACE FUNCTION cleanup_inactive_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE sessions
    SET is_active = FALSE
    WHERE last_activity < NOW() - INTERVAL '24 hours'
    AND is_active = TRUE;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Funktion: Session aktualisieren
CREATE OR REPLACE FUNCTION update_session_activity(p_session_token VARCHAR)
RETURNS VOID AS $$
BEGIN
    UPDATE sessions
    SET last_activity = NOW()
    WHERE session_token = p_session_token;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Kommentare für Dokumentation
-- ============================================================================
COMMENT ON TABLE sessions IS 'Verwaltet User-Sessions ohne Authentication';
COMMENT ON TABLE packing_lists IS 'Haupttabelle für generierte Packlisten mit AI-Content';
COMMENT ON TABLE chat_messages IS 'Chat-Verlauf für Nachfragen zu Packlisten';
COMMENT ON TABLE dummy_weather_data IS 'Vordefinierte Wetterdaten für Simulation';
COMMENT ON TABLE dummy_culture_tips IS 'Kultur-Tipps für verschiedene Destinationen';

COMMENT ON COLUMN packing_lists.items_json IS 'JSONB mit kategorisierter Packliste';
COMMENT ON COLUMN packing_lists.weather_info IS 'JSONB mit Wetterinformationen';
COMMENT ON COLUMN packing_lists.culture_tips IS 'Array mit Kultur-Hinweisen';
