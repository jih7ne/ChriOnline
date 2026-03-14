package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Adresse;
import com.chrionline.chrionline.server.services.AdresseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdresseController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(AdresseController.class);
    private final AdresseService adresseService;

    public AdresseController() {
        this.adresseService = AppConfig.getService(AdresseService.class);
    }

    // LISTER LES ADRESSES D'UN UTILISATEUR
    // INPUT  : { idUtilisateur }
    // OUTPUT : [ { id, rue, ville, code_postal, pays, est_principale } ]
    public String lister(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            if (idUtilisateur == null) {
                return AppResponse.badRequest("idUtilisateur est requis");
            }
            logger.info("Action: lister adresses utilisateur id={}", idUtilisateur);
            List<Adresse> adresses = adresseService.getAdressesUtilisateur(idUtilisateur);
            return AppResponse.success(adresses);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des adresses", e);
            return AppResponse.error("Erreur lors de la récupération des adresses");
        }
    }

    // AJOUTER UNE ADRESSE
    // INPUT  : { idUtilisateur, rue, complement, ville, code_postal, pays, est_principale }
    // OUTPUT : { message }
    public String ajouter(AppRequest request) {
        try {
            Adresse adresse = request.getPayloadAs(Adresse.class);
            if (adresse == null) {
                return AppResponse.badRequest("Les données de l'adresse sont requises");
            }
            logger.info("Action: ajouter adresse utilisateur id={}", adresse.getId_utilisateur());
            adresseService.ajouterAdresse(adresse);

            // Récupérer l'adresse avec son id depuis la BDD
            List<Adresse> adresses = adresseService.getAdressesUtilisateur(adresse.getId_utilisateur());
            Adresse adresseCreee = adresses.isEmpty() ? null : adresses.get(adresses.size() - 1);

            return AppResponse.success(adresseCreee, "Adresse ajoutée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout de l'adresse", e);
            return AppResponse.error("Erreur lors de l'ajout de l'adresse");
        }
    }

    // MODIFIER UNE ADRESSE
    // INPUT  : { id, rue, complement, ville, code_postal, pays, est_principale }
    // OUTPUT : { message }
    public String modifier(AppRequest request) {
        try {
            Integer id      = request.getInt("id");
            Adresse adresse = request.getPayloadAs(Adresse.class);
            if (id == null || adresse == null) {
                return AppResponse.badRequest("id et données de l'adresse sont requis");
            }
            logger.info("Action: modifier adresse id={}", id);
            adresseService.modifierAdresse(id, adresse);
            return AppResponse.success(null, "Adresse modifiée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la modification de l'adresse", e);
            return AppResponse.error("Erreur lors de la modification de l'adresse");
        }
    }

    // SUPPRIMER UNE ADRESSE
    // INPUT  : { id }
    // OUTPUT : { message }
    public String supprimer(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            if (id == null) {
                return AppResponse.badRequest("id est requis");
            }
            logger.info("Action: supprimer adresse id={}", id);
            adresseService.supprimerAdresse(id);
            return AppResponse.success(null, "Adresse supprimée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'adresse", e);
            return AppResponse.error("Erreur lors de la suppression de l'adresse");
        }
    }

    // DÉFINIR ADRESSE PRINCIPALE
    // INPUT  : { idUtilisateur, idAdresse }
    // OUTPUT : { message }
    public String setPrincipale(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            Integer idAdresse     = request.getInt("idAdresse");
            if (idUtilisateur == null || idAdresse == null) {
                return AppResponse.badRequest("idUtilisateur et idAdresse sont requis");
            }
            logger.info("Action: set adresse principale id={}", idAdresse);
            adresseService.setAdressePrincipale(idUtilisateur, idAdresse);
            return AppResponse.success(null, "Adresse principale mise à jour");
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour de l'adresse principale", e);
            return AppResponse.error("Erreur lors de la mise à jour");
        }
    }
}