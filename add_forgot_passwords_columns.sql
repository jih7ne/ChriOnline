ALTER TABLE Utilisateur
    ADD COLUMN question_secrete TEXT         NULL,
    ADD COLUMN reponse_secrete  VARCHAR(255)  NULL;