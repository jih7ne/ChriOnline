package com.chrionline.server.controllers;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.interfaces.IController;
import com.chrionline.core.security.OwnershipValidator;
import com.chrionline.core.utils.AuthorizationService;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.shared.models.Adresse;
import com.chrionline.shared.models.Utilisateur;
import com.chrionline.server.repositories.AdresseRepository;
import com.chrionline.server.services.AdresseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdresseController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(AdresseController.class);
    private final AdresseService adresseService;
    private final AdresseRepository adresseRepository;

    public AdresseController() {
        this.adresseService = ServerConfig.getService(AdresseService.class);
        this.adresseRepository = ServerConfig.getRepo(AdresseRepository.class);
    }

    // LISTER LES ADRESSES D'UN UTILISATEUR
    // ⭐ PRÉVENTION ESCALADE: Vérifier que l'utilisateur ne liste que SES propres adresses
    public String lister(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            if (idUtilisateur == null)
                return AppResponse.badRequest("idUtilisateur est requis");
            
            // ⭐ VÉRIFICATION OWNERSHIP: L'utilisateur authentifié ne peut voir que SES adresses
            String ownershipError = OwnershipValidator.validateOwnership(request, idUtilisateur, "adresse");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }
            
            logger.info(" Action: lister adresses utilisateur id={}", idUtilisateur);
            List<Adresse> adresses = adresseService.getAdressesUtilisateur(idUtilisateur);
            return AppResponse.success(adresses);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des adresses", e);
            return AppResponse.error("Erreur lors de la récupération des adresses");
        }
    }

    // AJOUTER UNE ADRESSE
    //  PRÉVENTION ESCALADE: Vérifier que l'adresse appartient à l'utilisateur authentifié
    public String ajouter(AppRequest request) {
        try {
            Adresse adresse = request.getPayloadAs(Adresse.class);
            if (adresse == null)
                return AppResponse.badRequest("Les données de l'adresse sont requises");
            
            // ⭐ VÉRIFICATION OWNERSHIP: L'utilisateur authentifié ne peut ajouter une adresse que pour LUI-MÊME
            String ownershipError = OwnershipValidator.validateOwnership(request, adresse.getId_utilisateur(), "adresse");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }
            
            logger.info(" Action: ajouter adresse utilisateur id={}", adresse.getId_utilisateur());
            adresseService.ajouterAdresse(adresse);
            List<Adresse> adresses = adresseService.getAdressesUtilisateur(adresse.getId_utilisateur());
            Adresse adresseCreee = adresses.isEmpty() ? null : adresses.get(adresses.size() - 1);
            return AppResponse.success(adresseCreee, "Adresse ajoutée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout de l'adresse", e);
            return AppResponse.error("Erreur lors de l'ajout de l'adresse");
        }
    }

    // MODIFIER UNE ADRESSE
    // ⭐ PRÉVENTION ESCALADE: Vérifier que l'adresse à modifier appartient à l'utilisateur authentifié
    public String modifier(AppRequest request) {
        try {
            Integer idAdresse = request.getInt("id");
            Adresse adresseData = request.getPayloadAs(Adresse.class);
            if (idAdresse == null || adresseData == null)
                return AppResponse.badRequest("id et données de l'adresse sont requis");
            
            // ⭐ VÉRIFICATION OWNERSHIP: Récupère l'adresse actuelle
            Adresse adresseExistante = adresseRepository.getAdresseById(idAdresse);
            if (adresseExistante == null) {
                return AppResponse.notFound("Adresse");
            }
            
            // ⭐ VÉRIFICATION OWNERSHIP: Valide que c'est l'adresse de l'utilisateur authentifié
            String ownershipError = OwnershipValidator.validateOwnership(request, adresseExistante.getId_utilisateur(), "adresse");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }
            
            logger.info(" Action: modifier adresse id={} utilisateur id={}", idAdresse, adresseExistante.getId_utilisateur());
            adresseService.modifierAdresse(idAdresse, adresseExistante.getId_utilisateur(), adresseData);
            return AppResponse.success(null, "Adresse modifiée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la modification de l'adresse", e);
            return AppResponse.error("Erreur lors de la modification de l'adresse");
        }
    }

    // SUPPRIMER UNE ADRESSE
    // PRÉVENTION ESCALADE: Vérifier que l'adresse à supprimer appartient à l'utilisateur authentifié
    public String supprimer(AppRequest request) {
        try {
            Integer idAdresse = request.getInt("id");
            if (idAdresse == null)
                return AppResponse.badRequest("id est requis");
            
            // VÉRIFICATION OWNERSHIP: Récupère l'adresse actuelle
            Adresse adresse = adresseRepository.getAdresseById(idAdresse);
            if (adresse == null) {
                return AppResponse.notFound("Adresse");
            }
            
            // ⭐ VÉRIFICATION OWNERSHIP: Valide que c'est l'adresse de l'utilisateur authentifié
            String ownershipError = OwnershipValidator.validateOwnership(request, adresse.getId_utilisateur(), "adresse");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }
            
            logger.info("Action: supprimer adresse id={} utilisateur id={}", idAdresse, adresse.getId_utilisateur());
            adresseService.supprimerAdresse(idAdresse, adresse.getId_utilisateur());
            return AppResponse.success(null, "Adresse supprimée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'adresse", e);
            return AppResponse.error("Erreur lors de la suppression de l'adresse");
        }
    }


    public String setPrincipale(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            Integer idAdresse     = request.getInt("idAdresse");
            if (idUtilisateur == null || idAdresse == null)
                return AppResponse.badRequest("idUtilisateur et idAdresse sont requis");

            //  VÉRIFICATION OWNERSHIP: Récupère l'adresse actuelle
            Adresse adresse = adresseRepository.getAdresseById(idAdresse);
            if (adresse == null) {
                return AppResponse.notFound("Adresse");
            }

            // ⭐ VÉRIFICATION OWNERSHIP: Valide que c'est l'adresse de l'utilisateur authentifié
            String ownershipError = OwnershipValidator.validateOwnership(request, adresse.getId_utilisateur(), "adresse");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }

            logger.info(" Action: setPrincipale adresse id={} pour utilisateur id={}", idAdresse, idUtilisateur);
            adresseService.setAdressePrincipale(idUtilisateur, idAdresse);
            return AppResponse.success(null, "Adresse principale mise à jour");
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour de l'adresse principale", e);
            return AppResponse.error("Erreur lors de la mise à jour");
        }
    }
    // ── fin C16 ───────────────────────────────────────────────────────────────
}
