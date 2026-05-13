CREATE TABLE user_devices (
                              id INT AUTO_INCREMENT PRIMARY KEY,

                              user_email VARCHAR(255) NOT NULL,

                              device_name VARCHAR(255) NOT NULL,

                              public_key TEXT NOT NULL,

                              fingerprint VARCHAR(128) NOT NULL,

                              key_algorithm VARCHAR(20) NOT NULL DEFAULT 'RSA',

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              last_used_at TIMESTAMP NULL DEFAULT NULL,

                              revoked BOOLEAN NOT NULL DEFAULT FALSE,

                              revoked_at TIMESTAMP NULL DEFAULT NULL,

    -- Indexes
                              UNIQUE KEY uk_fingerprint (fingerprint),
                              INDEX idx_user_email (user_email),
                              INDEX idx_user_revoked (user_email, revoked)
);