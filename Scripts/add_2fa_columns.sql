ALTER TABLE utilisateur
    ADD COLUMN two_factor_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN two_factor_secret   VARCHAR(64)  NULL,
    ADD COLUMN two_factor_verified BOOLEAN      NOT NULL DEFAULT FALSE;