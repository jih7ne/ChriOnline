-- Ajouter prixUnitaire pour sauvegarder le prix du produit au moment de la commande, indépendamment des prix actuels.
ALTER TABLE ligne_commande
ADD COLUMN prix_unitaire DECIMAL(10,2) NOT NULL;