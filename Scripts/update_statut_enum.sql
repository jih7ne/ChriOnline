ALTER TABLE Commande
MODIFY COLUMN statut ENUM('en_attente', 'validee', 'en_preparation', 'expediee', 'livree', 'annulee') DEFAULT 'en_attente';
