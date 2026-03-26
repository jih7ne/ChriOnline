
-- ChriOnline E-Commerce Database Schema
-- Script Final (avec toutes les mises à jour)

-- Table Utilisateur
CREATE TABLE Utilisateur (
                             id               INT PRIMARY KEY AUTO_INCREMENT,
                             nom              VARCHAR(100)  NOT NULL,
                             prenom           VARCHAR(100)  NOT NULL,
                             email            VARCHAR(150)  UNIQUE NOT NULL,
                             mot_de_passe     VARCHAR(255)  NOT NULL,
                             role             ENUM('client', 'admin') DEFAULT 'client',
                             statut           ENUM('actif', 'inactif') DEFAULT 'actif',
                             question_secrete TEXT          NULL,
                             reponse_secrete  VARCHAR(255)  NULL,
                             created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Table Categorie
CREATE TABLE Categorie (
                           id          INT PRIMARY KEY AUTO_INCREMENT,
                           nom         VARCHAR(100) NOT NULL,
                           description TEXT
);

-- Table Produit
CREATE TABLE Produit (
                         id           INT PRIMARY KEY AUTO_INCREMENT,
                         nom          VARCHAR(150)   NOT NULL,
                         description  TEXT,
                         prix         DECIMAL(10,2)  NOT NULL,
                         stock        INT            DEFAULT 0,
                         url_image    VARCHAR(255),
                         id_categorie INT,
                         date_ajout   DATETIME       DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (id_categorie) REFERENCES Categorie(id)
                             ON DELETE SET NULL
);

-- Table Adresse
CREATE TABLE Adresse (
                         id             INT PRIMARY KEY AUTO_INCREMENT,
                         id_utilisateur INT          NOT NULL,
                         rue            VARCHAR(255) NOT NULL,
                         complement     VARCHAR(255),
                         ville          VARCHAR(100) NOT NULL,
                         code_postal    VARCHAR(10)  NOT NULL,
                         pays           VARCHAR(100) NOT NULL DEFAULT 'Maroc',
                         est_principale TINYINT(1)   NOT NULL DEFAULT 0,
                         FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE
);

-- Table Panier
CREATE TABLE Panier (
                        id             INT PRIMARY KEY AUTO_INCREMENT,
                        id_utilisateur INT      NOT NULL,
                        date_creation  DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id)
                            ON DELETE CASCADE
);

-- Table Produit_Panier (relation produit-panier)
CREATE TABLE Produit_Panier (
                                id_panier  INT      NOT NULL,
                                id_produit INT      NOT NULL,
                                quantite   INT      NOT NULL DEFAULT 1,
                                date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (id_panier, id_produit),
                                FOREIGN KEY (id_panier)  REFERENCES Panier(id)  ON DELETE CASCADE,
                                FOREIGN KEY (id_produit) REFERENCES Produit(id) ON DELETE CASCADE
);

-- Table Commande
CREATE TABLE Commande (
                          id_commande    INT PRIMARY KEY AUTO_INCREMENT,
                          uuid_commande  VARCHAR(36)    UNIQUE NOT NULL,
                          id_utilisateur INT            NOT NULL,
                          id_panier      INT,
                          id_adresse     INT            NULL,
                          date           DATETIME       DEFAULT CURRENT_TIMESTAMP,
                          statut         ENUM('en_attente', 'validee', 'annulee') DEFAULT 'en_attente',
                          prix_total     DECIMAL(10,2)  NOT NULL,
                          FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE,
                          FOREIGN KEY (id_panier)      REFERENCES Panier(id)      ON DELETE SET NULL,
                          FOREIGN KEY (id_adresse)     REFERENCES Adresse(id)     ON DELETE SET NULL
);

-- Table Ligne_Commande
CREATE TABLE Ligne_Commande (
                                id            INT PRIMARY KEY AUTO_INCREMENT,
                                id_commande   INT           NOT NULL,
                                id_produit    INT           NOT NULL,
                                quantite      INT           NOT NULL,
                                prix_unitaire DECIMAL(10,2) NOT NULL,
                                FOREIGN KEY (id_commande) REFERENCES Commande(id_commande) ON DELETE CASCADE,
                                FOREIGN KEY (id_produit)  REFERENCES Produit(id)           ON DELETE CASCADE
);

-- Table Paiement
CREATE TABLE Paiement (
                          id                INT PRIMARY KEY AUTO_INCREMENT,
                          id_commande       INT          NOT NULL UNIQUE,
                          date_paiement     DATETIME     DEFAULT CURRENT_TIMESTAMP,
                          methode_paiement  ENUM('carte_bancaire') NOT NULL,
                          statut            ENUM('en_attente', 'confirme', 'echoue') DEFAULT 'en_attente',
                          numero_masque     VARCHAR(4),
                          FOREIGN KEY (id_commande) REFERENCES Commande(id_commande) ON DELETE CASCADE
);

-- Table Notification
CREATE TABLE notification (
                              id                INT PRIMARY KEY AUTO_INCREMENT,
                              notification_uuid VARCHAR(36)  NOT NULL UNIQUE,
                              id_utilisateur    INT          NOT NULL,
                              notification_type ENUM('INFO', 'WARNING', 'ERROR', 'SUCCESS', 'REMINDER', 'ALERT') NOT NULL DEFAULT 'INFO',
                              severity          ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'LOW',
                              title             VARCHAR(255) NOT NULL,
                              message           TEXT         NOT NULL,
                              data              JSON,
                              source            VARCHAR(100),
                              version           INT          DEFAULT 1,

    -- Timestamps
                              date_creation     DATETIME     DEFAULT CURRENT_TIMESTAMP,
                              date_envoi        DATETIME,
                              date_lue          DATETIME,
                              date_expiration   DATETIME,

    -- Status tracking
                              statut            ENUM('en_attente', 'envoyee', 'lue', 'archivee', 'echouee') DEFAULT 'en_attente',
                              priorite          INT          DEFAULT 0,

    -- Delivery tracking
                              tentative_envoi   INT          DEFAULT 0,
                              derniere_tentative DATETIME,

    -- Metadata
                              metadata          JSON,

                              FOREIGN KEY (id_utilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE,

                              INDEX idx_utilisateur_statut (id_utilisateur, statut),
                              INDEX idx_date_envoi         (date_envoi),
                              INDEX idx_type_severity      (notification_type, severity),
                              INDEX idx_expiration         (date_expiration)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;