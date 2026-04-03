CREATE TABLE IF NOT EXISTS session (
                                       id              INT PRIMARY KEY AUTO_INCREMENT,
                                       ip_address      VARCHAR(45)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    locked_until    DATETIME     NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_ip (ip_address),
    INDEX idx_locked (locked_until)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;