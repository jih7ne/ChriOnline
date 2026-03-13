package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.core.enums.MethodePaiement;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Paiement;
import com.chrionline.chrionline.server.services.PaiementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaiementController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(PaiementController.class);
    private final PaiementService paiementService;

    public PaiementController() {
        this.paiementService = AppConfig.getService(PaiementService.class);
    }

    // TRAITER UN PAIEMENT
    // INPUT  : { idCommande, numeroCarte, cvv, dateExpiration, methodePaiement }
    // OUTPUT : { statut, numeroMasque }
    public String traiter(AppRequest request) {
        try {
            Integer idCommande      = request.getInt("idCommande");
            String  numeroCarte     = request.getString("numeroCarte");
            String  cvv             = request.getString("cvv");
            String  dateExpiration  = request.getString("dateExpiration");
            String  methodeStr      = request.getString("methodePaiement");

            // Validation des champs requis
            if (idCommande == null || numeroCarte == null || cvv == null || dateExpiration == null) {
                return AppResponse.badRequest("idCommande, numeroCarte, cvv et dateExpiration sont requis");
            }

            // Conversion String -> enum MethodePaiement (défaut : CARTE_BANCAIRE)
            MethodePaiement methode;
            try {
                methode = (methodeStr != null)
                        ? MethodePaiement.valueOf(methodeStr)
                        : MethodePaiement.CARTE_BANCAIRE;
            } catch (IllegalArgumentException e) {
                return AppResponse.badRequest("Méthode de paiement invalide : " + methodeStr);
            }

            logger.info("Action: traiter paiement commande id={} méthode={}", idCommande, methode);

            // Validation du format avant traitement
            String erreurValidation = paiementService.validerPaiement(numeroCarte, cvv, dateExpiration);
            if (erreurValidation != null) {
                logger.warn("Paiement refusé commande id={} : {}", idCommande, erreurValidation);

                java.util.Map<String, Object> echec = new java.util.HashMap<>();
                echec.put("statut",  "ECHOUE");
                echec.put("message", erreurValidation);

                return AppResponse.success(echec, "Paiement refusé");
            }

            // Traitement complet (enregistrement + confirmation commande)
            boolean succes = paiementService.enregistrerPaiement(
                    idCommande, numeroCarte, cvv, dateExpiration, methode
            );

            // Récupération du paiement enregistré pour retourner le numéro masqué
            Paiement paiement = paiementService.getPaiementByCommande(idCommande);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("statut",       succes ? "CONFIRME" : "ECHOUE");
            result.put("numeroMasque", paiement != null ? "**** **** **** " + paiement.getNumero_masque() : null);
            result.put("idCommande",   idCommande);

            String message = succes ? "Paiement accepté" : "Paiement refusé";
            return AppResponse.success(result, message);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du paiement", e);
            return AppResponse.error("Erreur lors du traitement du paiement");
        }
    }

    // RÉCUPÉRER LE PAIEMENT D'UNE COMMANDE
    // INPUT  : { idCommande }
    // OUTPUT : { paiement }
    public String getPaiement(AppRequest request) {
        try {
            Integer idCommande = request.getInt("idCommande");
            if (idCommande == null) {
                return AppResponse.badRequest("idCommande est requis");
            }

            logger.info("Action: récupérer paiement commande id={}", idCommande);

            Paiement paiement = paiementService.getPaiementByCommande(idCommande);
            if (paiement == null) {
                return AppResponse.notFound("Paiement");
            }

            return AppResponse.success(paiement);

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du paiement", e);
            return AppResponse.error("Erreur lors de la récupération du paiement");
        }
    }
}