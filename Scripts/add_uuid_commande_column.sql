-- Ajouter colonne uuid_commande dans la table commande
ALTER TABLE commande
ADD COLUMN uuid_commande VARCHAR(36) UNIQUE NOT NULL;
