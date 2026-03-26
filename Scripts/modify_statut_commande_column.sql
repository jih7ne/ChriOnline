ALTER TABLE Commande
MODIFY COLUMN statut ENUM('en_attente', 'validee', 'annulee') DEFAULT 'en_attente';