package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.server.data.models.Adresse;
import com.chrionline.chrionline.server.repositories.AdresseRepository;
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

    public void modifierAdresse(int id, Adresse adresse) {
        logger.info("Modification adresse id={}", id);
        adresseRepository.update(id, adresse);
    }

    public void supprimerAdresse(int id) {
        logger.info("Suppression adresse id={}", id);
        adresseRepository.delete(id);
    }

    public void setAdressePrincipale(int idUtilisateur, int idAdresse) {
        logger.info("Set adresse principale id={} pour utilisateur id={}", idAdresse, idUtilisateur);
        adresseRepository.setAdressePrincipale(idUtilisateur, idAdresse);
    }
}