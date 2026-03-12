

-- Table Adresse
CREATE TABLE Adresse (
                         id              INT PRIMARY KEY AUTO_INCREMENT,
                         id_utilisateur  INT NOT NULL,
                         rue             VARCHAR(255) NOT NULL,
                         complement      VARCHAR(255),
                         ville           VARCHAR(100) NOT NULL,
                         code_postal     VARCHAR(10)  NOT NULL,
                         pays            VARCHAR(100) NOT NULL DEFAULT 'Maroc',
                         est_principale  TINYINT(1)   NOT NULL DEFAULT 0,
                         FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE
);

-- Référence à l'adresse de livraison dans Commande
ALTER TABLE Commande
    ADD COLUMN id_adresse INT NULL,
    ADD CONSTRAINT fk_commande_adresse
        FOREIGN KEY (id_adresse) REFERENCES Adresse(id) ON DELETE SET NULL;