package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Commande;
import com.chrionline.chrionline.server.data.models.LigneCommande;
import com.chrionline.chrionline.server.services.CommandeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CommandeController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(CommandeController.class);
    private final CommandeService commandeService;

    public CommandeController() {
        this.commandeService = AppConfig.getService(CommandeService.class);
    }

    // VALIDER UNE COMMANDE
    // INPUT  : { idUtilisateur, idAdresse, lignes: [{id_produit, quantite, prix_unitaire}] }
    // OUTPUT : { uuidCommande, idCommande, statut }
    public String valider(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            Integer idAdresse     = request.getInt("idAdresse");
            List<LigneCommande> lignes = request.getPayloadAsList(LigneCommande.class);

            if (idUtilisateur == null || idAdresse == null) {
                return AppResponse.badRequest("idUtilisateur et idAdresse sont requis");
            }
            if (lignes == null || lignes.isEmpty()) {
                return AppResponse.badRequest("La commande doit contenir au moins une ligne");
            }

            logger.info("Action: valider commande utilisateur id={}", idUtilisateur);

            Commande commande = commandeService.validerCommande(idUtilisateur, idAdresse, lignes);

            if (commande == null) {
                return AppResponse.error("Validation échouée : stock insuffisant ou produit introuvable");
            }

            // Réponse avec les infos essentielles pour le client
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("uuidCommande", commande.getUuid_commande());
            result.put("idCommande",   commande.getId_commande());
            result.put("statut",       commande.getStatut());
            result.put("prixTotal",    commande.getPrix_total());

            return AppResponse.success(result, "Commande validée avec succès");

        } catch (Exception e) {
            logger.error("Erreur lors de la validation de la commande", e);
            return AppResponse.error("Erreur lors de la validation de la commande");
        }
    }

    // LISTER LES COMMANDES D'UN UTILISATEUR
    // INPUT  : { idUtilisateur }
    // OUTPUT : [ { idCommande, uuidCommande, statut, prixTotal, date } ]
    public String lister(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            if (idUtilisateur == null) {
                return AppResponse.badRequest("idUtilisateur est requis");
            }

            logger.info("Action: lister commandes utilisateur id={}", idUtilisateur);

            List<Commande> commandes = commandeService.getHistoriqueCommandes(idUtilisateur);
            return AppResponse.success(commandes, "Commandes récupérées avec succès");

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des commandes", e);
            return AppResponse.error("Erreur lors de la récupération des commandes");
        }
    }

    // DÉTAILS D'UNE COMMANDE + SES LIGNES
    // INPUT  : { idCommande }
    // OUTPUT : { commande, lignes }
    public String details(AppRequest request) {
        try {
            Integer idCommande = request.getInt("idCommande");
            if (idCommande == null) {
                return AppResponse.badRequest("idCommande est requis");
            }

            logger.info("Action: détails commande id={}", idCommande);

            List<LigneCommande> lignes = commandeService.getLignesCommande(idCommande);
            if (lignes == null) {
                return AppResponse.notFound("Commande");
            }

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("idCommande", idCommande);
            result.put("lignes",     lignes);

            return AppResponse.success(result);

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des détails de la commande", e);
            return AppResponse.error("Erreur lors de la récupération des détails de la commande");
        }
    }

    // ANNULER UNE COMMANDE
    // INPUT  : { idCommande }
    // OUTPUT : { message }
    public String annuler(AppRequest request) {
        try {
            Integer idCommande = request.getInt("idCommande");
            if (idCommande == null) {
                return AppResponse.badRequest("idCommande est requis");
            }

            logger.info("Action: annuler commande id={}", idCommande);
            commandeService.annulerCommande(idCommande);
            return AppResponse.success(null, "Commande annulée avec succès");

        } catch (Exception e) {
            logger.error("Erreur lors de l'annulation de la commande", e);
            return AppResponse.error("Erreur lors de l'annulation de la commande");
        }
    }
}