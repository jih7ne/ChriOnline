-- =============================================
-- ChriOnline E-Commerce Database Schema
-- =============================================

-- Table Utilisateur
CREATE TABLE Utilisateur (
                             id INT PRIMARY KEY AUTO_INCREMENT,
                             nom VARCHAR(100) NOT NULL,
                             prenom VARCHAR(100) NOT NULL,
                             email VARCHAR(150) UNIQUE NOT NULL,
                             mot_de_passe VARCHAR(255) NOT NULL,
                             role ENUM('client', 'admin') DEFAULT 'client',
                             statut ENUM('actif', 'inactif') DEFAULT 'actif'
);

-- Table Categorie
CREATE TABLE Categorie (
                           id INT PRIMARY KEY AUTO_INCREMENT,
                           nom VARCHAR(100) NOT NULL,
                           description TEXT
);

-- Table Produit
CREATE TABLE Produit (
                         id INT PRIMARY KEY AUTO_INCREMENT,
                         nom VARCHAR(150) NOT NULL,
                         description TEXT,
                         prix DECIMAL(10,2) NOT NULL,
                         stock INT DEFAULT 0,
                         url_image VARCHAR(255),
                         id_categorie INT,
                         FOREIGN KEY (id_categorie) REFERENCES Categorie(id)
                             ON DELETE SET NULL
);

-- Table Panier
CREATE TABLE Panier (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        id_utilisateur INT NOT NULL,
                        date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id)
                            ON DELETE CASCADE
);

-- Table Produit_Panier (relation produit-panier)
CREATE TABLE Produit_Panier (
                                id_panier INT NOT NULL,
                                id_produit INT NOT NULL,
                                quantite INT NOT NULL DEFAULT 1,
                                PRIMARY KEY (id_panier, id_produit),
                                FOREIGN KEY (id_panier) REFERENCES Panier(id)
                                    ON DELETE CASCADE,
                                FOREIGN KEY (id_produit) REFERENCES Produit(id)
                                    ON DELETE CASCADE
);

-- Table Commande
CREATE TABLE Commande (
                          id_commande INT PRIMARY KEY AUTO_INCREMENT,
                          id_utilisateur INT NOT NULL,
                          id_panier INT,
                          date DATETIME DEFAULT CURRENT_TIMESTAMP,
                          statut ENUM('en_attente', 'validee', 'expediee', 'livree', 'annulee') DEFAULT 'en_attente',
                          prix_total DECIMAL(10,2) NOT NULL,
                          FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id)
                              ON DELETE CASCADE,
                          FOREIGN KEY (id_panier) REFERENCES Panier(id)
                              ON DELETE SET NULL
);

-- Table Ligne_Commande (détails de chaque commande)
CREATE TABLE Ligne_Commande (
                                id INT PRIMARY KEY AUTO_INCREMENT,
                                id_commande INT NOT NULL,
                                id_produit INT NOT NULL,
                                quantite INT NOT NULL,
                                FOREIGN KEY (id_commande) REFERENCES Commande(id_commande)
                                    ON DELETE CASCADE,
                                FOREIGN KEY (id_produit) REFERENCES Produit(id)
                                    ON DELETE CASCADE
);

-- Table Paiement
CREATE TABLE Paiement (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          id_commande INT NOT NULL UNIQUE,
                          date_paiement DATETIME DEFAULT CURRENT_TIMESTAMP,
                          methode_paiement ENUM('carte_bancaire', 'paypal', 'fictif') NOT NULL,
                          statut ENUM('en_attente', 'confirme', 'echoue') DEFAULT 'en_attente',
                          FOREIGN KEY (id_commande) REFERENCES Commande(id_commande)
                              ON DELETE CASCADE
);

-- Table Notification
CREATE TABLE Notification (
                              id INT PRIMARY KEY AUTO_INCREMENT,
                              id_utilisateur INT NOT NULL,
                              id_commande INT NOT NULL,
                              message TEXT NOT NULL,
                              date_envoi DATETIME DEFAULT CURRENT_TIMESTAMP,
                              statut ENUM('lue', 'non_lue') DEFAULT 'non_lue',
                              FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id)
                                  ON DELETE CASCADE,
                              FOREIGN KEY (id_commande) REFERENCES Commande(id_commande)
                                  ON DELETE CASCADE
);