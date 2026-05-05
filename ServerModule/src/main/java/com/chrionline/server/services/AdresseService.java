package com.chrionline.server.services;

import com.chrionline.shared.models.Adresse;
import com.chrionline.server.repositories.AdresseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdresseService {
    private static final Logger logger = LoggerFactory.getLogger(AdresseService.class);
    private final AdresseRepository adresseRepository;

    public AdresseService(AdresseRepository adresseRepository) {
        this.adresseRepository = adresseRepository;
        logger.info("AdresseService initialized");
    }

    public List<Adresse> getAdressesUtilisateur(int idUtilisateur) {
        logger.info("Récupération adresses utilisateur id={}", idUtilisateur);
        return adresseRepository.getAdressesUtilisateur(idUtilisateur);
    }

    public void ajouterAdresse(Adresse adresse) {
        logger.info("Ajout adresse utilisateur id={}", adresse.getId_utilisateur());
        adresseRepository.add(adresse);
    }

    public void modifierAdresse(int id, int idUtilisateur, Adresse adresse) {
        logger.info("Modification adresse id={} pour utilisateur id={}", id, idUtilisateur);
        // 🔒 SÉCURITÉ IDOR: On vérifie que l'adresse appartient bien à l'utilisateur avant de modifier
        Adresse existing = adresseRepository.getAdresseByIdAndUser(id, idUtilisateur);
        if (existing == null) {
            logger.warn("Tentative de modification d'une adresse (id={}) n'appartenant pas à l'utilisateur {}", id, idUtilisateur);
            throw new com.chrionline.core.exceptions.BusinessException("Adresse introuvable ou accès refusé");
        }
        adresseRepository.update(id, adresse);
    }

    public void supprimerAdresse(int id, int idUtilisateur) {
        logger.info("Suppression adresse id={} pour utilisateur id={}", id, idUtilisateur);
        // 🔒 SÉCURITÉ IDOR: On utilise le deleteScoped pour garantir l'ownership
        adresseRepository.deleteScoped(id, idUtilisateur);
    }

    public void setAdressePrincipale(int idUtilisateur, int idAdresse) {
        logger.info("Set adresse principale id={} pour utilisateur id={}", idAdresse, idUtilisateur);
        adresseRepository.setAdressePrincipale(idUtilisateur, idAdresse);
    }
}
