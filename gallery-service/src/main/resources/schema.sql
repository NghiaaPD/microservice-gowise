-- Schema for travel_photos table
CREATE TABLE IF NOT EXISTS travel_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    trip_id UUID,
    file_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption TEXT,
    location TEXT,
    like_count INT DEFAULT 0,
    taken_at TIMESTAMP,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    is_public BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_travel_photos_user_id ON travel_photos (user_id);

CREATE INDEX IF NOT EXISTS idx_travel_photos_trip_id ON travel_photos (trip_id);

CREATE INDEX IF NOT EXISTS idx_travel_photos_user_trip ON travel_photos (user_id, trip_id);