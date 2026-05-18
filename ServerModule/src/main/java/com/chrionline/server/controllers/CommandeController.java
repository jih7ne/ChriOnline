package com.chrionline.server.controllers;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.enums.StatutCommande;
import com.chrionline.core.interfaces.IController;
import com.chrionline.core.security.OwnershipValidator;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.core.utils.AuthorizationService;
import com.chrionline.shared.models.Commande;
import com.chrionline.shared.models.LigneCommande;
import com.chrionline.shared.models.Utilisateur;
import com.chrionline.server.repositories.CommandeRepository;
import com.chrionline.server.services.CommandeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CommandeController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(CommandeController.class);
    private final CommandeService commandeService;
    private final CommandeRepository commandeRepository;

    public CommandeController() {
        this.commandeService = ServerConfig.getService(CommandeService.class);
        this.commandeRepository = ServerConfig.getRepo(CommandeRepository.class);
    }

    // VALIDER UNE COMMANDE
    // INPUT  : { idUtilisateur, idAdresse, lignes: [{id_produit, quantite, prix_unitaire}] }
    // OUTPUT : { uuidCommande, idCommande, statut }
    // ⭐ PRÉVENTION ESCALADE: Vérifier que idUtilisateur correspond à l'utilisateur authentifié
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

            // ⭐ VÉRIFICATION OWNERSHIP: L'utilisateur authentifié ne peut commander que pour LUI-MÊME
            String ownershipError = OwnershipValidator.validateOwnership(request, idUtilisateur, "commande");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }

            logger.info("✅ Action: valider commande utilisateur id={}", idUtilisateur);

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
    // ⭐ PRÉVENTION ESCALADE: Vérifier que l'utilisateur ne liste que SES propres commandes
    public String lister(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null || !payloadMap.containsKey("idUtilisateur")) {
                return AppResponse.badRequest("idUtilisateur est requis");
            }
            Integer idUtilisateur = ((Number) payloadMap.get("idUtilisateur")).intValue();

            // ⭐ VÉRIFICATION OWNERSHIP: L'utilisateur authentifié ne peut voir que SES commandes
            String ownershipError = OwnershipValidator.validateOwnership(request, idUtilisateur, "commande");
            if (ownershipError != null) {
                return AppResponse.forbidden(ownershipError);
            }

            logger.info("✅ Action: lister commandes utilisateur id={}", idUtilisateur);

            List<Commande> commandes = commandeService.getHistoriqueCommandes(idUtilisateur);
            return AppResponse.success(commandes, "Commandes récupérées avec succès");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Erreur critique lors de la récupération des commandes pour utilisateur: " + e.getMessage(), e);
            return AppResponse.error("Erreur lors de la récupération des commandes: " + e.getMessage());
        }
    }

    // DÉTAILS D'UNE COMMANDE + SES LIGNES (avec nom produit)
    // INPUT  : { idCommande }
    // OUTPUT : { idCommande, lignes: [{id, id_produit, quantite, prix_unitaire, nom_produit}] }
    // ⭐ PRÉVENTION ESCALADE: Vérifier que la commande appartient à l'utilisateur authentifié
    public String details(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null || !payloadMap.containsKey("idCommande")) {
                return AppResponse.badRequest("idCommande est requis");
            }
            Integer idCommande = ((Number) payloadMap.get("idCommande")).intValue();

            logger.info("Action: détails commande id={}", idCommande);

            // ⭐ SÉCURITÉ IDOR: On récupère l'utilisateur authentifié
            Utilisateur authenticatedUser = AuthorizationService.getAuthenticatedUser(request);
            if (authenticatedUser == null) {
                return AppResponse.forbidden("Authentification requise");
            }
 
            Commande commande;
            if ("ADMIN".equalsIgnoreCase(authenticatedUser.getRole())) {
                // L'admin peut voir toutes les commandes
                commande = commandeRepository.getCommandeById(idCommande);
            } else {
                // ⭐ SÉCURITÉ IDOR: On récupère la commande UNIQUEMENT si elle appartient à l'utilisateur
                commande = commandeRepository.getCommandeByIdAndUser(idCommande, authenticatedUser.getId());
            }

            if (commande == null) {
                return AppResponse.notFound("Commande");
            }
 
            logger.info("Action: détails commande id={} pour utilisateur id={}", idCommande, authenticatedUser.getId());
            List<java.util.Map<String, Object>> lignes = commandeService.getLignesAvecNom(idCommande);
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
    // ⭐ PRÉVENTION ESCALADE: Vérifier que la commande appartient à l'utilisateur authentifié
    public String annuler(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            Integer idCommande = null;
            if (payloadMap != null && payloadMap.get("idCommande") != null) {
                idCommande = ((Number) payloadMap.get("idCommande")).intValue();
            } else {
                idCommande = request.getInt("idCommande");
            }

            if (idCommande == null) {
                return AppResponse.badRequest("idCommande est requis");
            }

            // ⭐ SÉCURITÉ IDOR: On récupère l'utilisateur authentifié
            Utilisateur authenticatedUser = AuthorizationService.getAuthenticatedUser(request);
            if (authenticatedUser == null) {
                return AppResponse.forbidden("Authentification requise");
            }
 
            // ⭐ SÉCURITÉ IDOR: On récupère la commande UNIQUEMENT si elle appartient à l'utilisateur
            Commande commande = commandeRepository.getCommandeByIdAndUser(idCommande, authenticatedUser.getId());
            if (commande == null) {
                return AppResponse.notFound("Commande");
            }
 
            logger.info("✅ Action: annuler commande id={} utilisateur id={}", idCommande, authenticatedUser.getId());
 
            boolean succes = commandeService.annulerCommande(idCommande, authenticatedUser.getId());

            if (!succes) {
                return AppResponse.error("Impossible d'annuler cette commande. Elle est peut-être déjà validée ou n'existe pas.");
            }

            return AppResponse.success(null, "Commande annulée avec succès");

        } catch (Exception e) {
            logger.error("Erreur lors de l'annulation de la commande", e);
            return AppResponse.error("Erreur lors de l'annulation de la commande");
        }
    }

    // CHANGER LE STATUT D'UNE COMMANDE (admin)
    // INPUT  : { idCommande, statut }
    // OUTPUT : { message }
    // UDP    : Scénario 4 — notifie le client concerné
    public String changerStatut(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null
                    || !payloadMap.containsKey("idCommande")
                    || !payloadMap.containsKey("statut")) {
                return AppResponse.badRequest("idCommande et statut sont requis");
            }

            int idCommande = ((Number) payloadMap.get("idCommande")).intValue();
            String statutStr = String.valueOf(payloadMap.get("statut")).toUpperCase();

            StatutCommande newStatut;
            try {
                newStatut = StatutCommande.valueOf(statutStr);
            } catch (IllegalArgumentException ex) {
                return AppResponse.badRequest("Statut invalide : " + statutStr);
            }

            logger.info("Action: changerStatut commande id={} → {}", idCommande, newStatut);

            boolean succes = commandeService.changerStatutCommande(idCommande, newStatut);

            if (!succes) {
                return AppResponse.error("Impossible de changer le statut.");
            }

            return AppResponse.success(null, "Statut mis à jour avec succès");

        } catch (Exception e) {
            logger.error("Erreur lors du changement de statut", e);
            return AppResponse.error("Erreur lors du changement de statut");
        }
    }
}
