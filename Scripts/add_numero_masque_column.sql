-- Ajouter colonne numero_masque dans la table paiement pour stocker les 4 derniers chiffres de la carte pour traçabilité et audit, sans exposer le numéro complet pour des raisons de sécurité
ALTER TABLE paiement
ADD COLUMN numero_masque VARCHAR(4);