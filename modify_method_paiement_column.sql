ALTER TABLE Paiement
    MODIFY COLUMN methode_paiement ENUM('carte_bancaire') NOT NULL;