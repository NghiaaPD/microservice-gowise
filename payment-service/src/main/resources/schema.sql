CREATE TABLE IF NOT EXISTS payment_records (
    id UUID PRIMARY KEY,
    order_code BIGINT NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    description VARCHAR(255),
    payment_link_id VARCHAR(128),
    status VARCHAR(64),
    checkout_url TEXT,
    qr_code TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE payment_records
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
