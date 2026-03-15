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
            // First, get the entire payload as a Map
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null) {
                return AppResponse.badRequest("Payload is required");
            }

            Number numUser = (Number) payloadMap.get("idUtilisateur");
            Number numAdresse = (Number) payloadMap.get("idAdresse");
            Integer idUtilisateur = numUser != null ? numUser.intValue() : null;
            Integer idAdresse     = numAdresse != null ? numAdresse.intValue() : null;
            
            // Extract the 'lignes' array from the nested JSON object
            java.util.List<java.util.Map<String, Object>> lignesBrutes = (java.util.List<java.util.Map<String, Object>>) payloadMap.get("lignes");
            List<LigneCommande> lignes = null;
            if (lignesBrutes != null) {
                lignes = new java.util.ArrayList<>();
                for (java.util.Map<String, Object> map : lignesBrutes) {
                    LigneCommande lc = new LigneCommande();
                    if (map.containsKey("id_produit")) lc.setId_produit(((Number) map.get("id_produit")).intValue());
                    if (map.containsKey("quantite")) lc.setQuantite(((Number) map.get("quantite")).intValue());
                    if (map.containsKey("prix_unitaire")) lc.setPrix_unitaire(((Number) map.get("prix_unitaire")).doubleValue());
                    lignes.add(lc);
                }
            }

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
            System.out.println("EXCEPTION DANS LE CONTROLEUR VALIDER :");
            e.printStackTrace(System.out);
            logger.error("Erreur lors de la validation de la commande", e);
            return AppResponse.error("Erreur lors de la validation de la commande");
        }
    }

    // LISTER LES COMMANDES D'UN UTILISATEUR
    // INPUT  : { idUtilisateur }
    // OUTPUT : [ { idCommande, uuidCommande, statut, prixTotal, date } ]
    public String lister(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null || !payloadMap.containsKey("idUtilisateur")) {
                return AppResponse.badRequest("idUtilisateur est requis");
            }
            Integer idUtilisateur = ((Number) payloadMap.get("idUtilisateur")).intValue();

            logger.info("Action: lister commandes utilisateur id={}", idUtilisateur);

            List<Commande> commandes = commandeService.getHistoriqueCommandes(idUtilisateur);
            return AppResponse.success(commandes, "Commandes récupérées avec succès");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Erreur critique lors de la récupération des commandes pour utilisateur: " + e.getMessage(), e);
            return AppResponse.error("Erreur lors de la récupération des commandes: " + e.getMessage());
        }
    }

    // DÉTAILS D'UNE COMMANDE + SES LIGNES
    // INPUT  : { idCommande }
    // OUTPUT : { commande, lignes }
    public String details(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null || !payloadMap.containsKey("idCommande")) {
                return AppResponse.badRequest("idCommande est requis");
            }
            Integer idCommande = ((Number) payloadMap.get("idCommande")).intValue();

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