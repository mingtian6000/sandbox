-- Create verification_codes table
CREATE TABLE IF NOT EXISTS verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    verification_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    is_used BOOLEAN DEFAULT FALSE
);

-- Create indexes for faster lookups
CREATE INDEX idx_verification_codes_email ON verification_codes(email);
CREATE INDEX idx_verification_codes_email_type ON verification_codes(email, verification_type);
CREATE INDEX idx_verification_codes_created_at ON verification_codes(created_at);
CREATE INDEX idx_verification_codes_expires_at ON verification_codes(expires_at);

-- Create partial index for active (unused and not expired) codes
CREATE INDEX idx_verification_codes_active ON verification_codes(email, verification_type)
    WHERE is_used = FALSE AND expires_at > CURRENT_TIMESTAMP;