package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Panier;
import com.chrionline.chrionline.server.services.PanierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PanierController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(PanierController.class);
    private final PanierService panierService;

    public PanierController() {
        this.panierService = AppConfig.getService(PanierService.class);
    }

    //  OBTENIR LE PANIER
    public String getPanier(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            if (idUtilisateur == null) {
                return AppResponse.badRequest("L'identifiant utilisateur est requis");
            }
            logger.info("Action: getPanier utilisateur id={}", idUtilisateur);
            Panier panier = panierService.getPanier(idUtilisateur);
            return AppResponse.success(panier);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du panier", e);
            return AppResponse.error("Erreur lors de la récupération du panier");
        }
    }

    //  AJOUTER UN PRODUIT
    public String ajouterProduit(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            Integer idProduit = request.getInt("idProduit");
            Integer quantite = request.getInt("quantite");
            if (idUtilisateur == null || idProduit == null || quantite == null) {
                return AppResponse.badRequest("idUtilisateur, idProduit et quantite sont requis");
            }
            logger.info("Action: ajouterProduit id={} au panier utilisateur id={}",
                    idProduit, idUtilisateur);
            boolean succes = panierService.ajouterProduit(idUtilisateur, idProduit, quantite);
            if (!succes) {
                return AppResponse.error("Stock insuffisant ou produit non trouvé");
            }
            return AppResponse.success(null, "Produit ajouté au panier avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout au panier", e);
            return AppResponse.error("Erreur lors de l'ajout au panier");
        }
    }

    //  SUPPRIMER UN PRODUIT
    public String supprimerProduit(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            Integer idProduit = request.getInt("idProduit");
            if (idUtilisateur == null || idProduit == null) {
                return AppResponse.badRequest("idUtilisateur et idProduit sont requis");
            }
            logger.info("Action: supprimerProduit id={} du panier utilisateur id={}",
                    idProduit, idUtilisateur);
            panierService.supprimerProduit(idUtilisateur, idProduit);
            return AppResponse.success(null, "Produit supprimé du panier avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du panier", e);
            return AppResponse.error("Erreur lors de la suppression du panier");
        }
    }

    //  MODIFIER QUANTITE
    public String modifierQuantite(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            Integer idProduit = request.getInt("idProduit");
            Integer nouvelleQuantite = request.getInt("quantite");
            if (idUtilisateur == null || idProduit == null || nouvelleQuantite == null) {
                return AppResponse.badRequest("idUtilisateur, idProduit et quantite sont requis");
            }
            logger.info("Action: modifierQuantite produit id={} à {} pour utilisateur id={}",
                    idProduit, nouvelleQuantite, idUtilisateur);
            boolean succes = panierService.modifierQuantite(idUtilisateur, idProduit, nouvelleQuantite);
            if (!succes) {
                return AppResponse.error("Stock insuffisant");
            }
            return AppResponse.success(null, "Quantité modifiée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la modification de la quantité", e);
            return AppResponse.error("Erreur lors de la modification de la quantité");
        }
    }

    //  CALCUL TOTAL
    public String calculerTotal(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            if (idUtilisateur == null) {
                return AppResponse.badRequest("L'identifiant utilisateur est requis");
            }
            logger.info("Action: calculerTotal panier utilisateur id={}", idUtilisateur);
            double total = panierService.calculerTotal(idUtilisateur);
            return AppResponse.success(total, "Total calculé avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors du calcul du total", e);
            return AppResponse.error("Erreur lors du calcul du total");
        }
    }

    //  VIDER LE PANIER
    public String viderPanier(AppRequest request) {
        try {
            Integer idUtilisateur = request.getInt("idUtilisateur");
            if (idUtilisateur == null) {
                return AppResponse.badRequest("L'identifiant utilisateur est requis");
            }
            logger.info("Action: viderPanier utilisateur id={}", idUtilisateur);
            panierService.viderPanier(idUtilisateur);
            return AppResponse.success(null, "Panier vidé avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors du vidage du panier", e);
            return AppResponse.error("Erreur lors du vidage du panier");
        }
    }
}