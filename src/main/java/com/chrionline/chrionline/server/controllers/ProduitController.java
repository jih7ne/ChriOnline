package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Produit;
import com.chrionline.chrionline.server.services.ProduitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProduitController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(ProduitController.class);
    private final ProduitService produitService;

    public ProduitController() {
        this.produitService = AppConfig.getService(ProduitService.class);
    }

    //  LISTER TOUS LES PRODUITS
    public String lister(AppRequest request) {
        try {
            logger.info("Action: lister tous les produits");
            List<Produit> produits = produitService.listerProduits();
            return AppResponse.success(produits, "Produits récupérés avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la liste des produits", e);
            return AppResponse.error("Erreur lors de la récupération des produits");
        }
    }

    //  DETAILS D'UN PRODUIT
    public String details(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            if (id == null) {
                return AppResponse.badRequest("L'identifiant du produit est requis");
            }
            logger.info("Action: détails produit id={}", id);
            Produit produit = produitService.getProduitById(id);
            if (produit == null) {
                return AppResponse.notFound("Produit");
            }
            return AppResponse.success(produit);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du produit", e);
            return AppResponse.error("Erreur lors de la récupération du produit");
        }
    }

    //  RECHERCHE PAR CATEGORIE
    public String parCategorie(AppRequest request) {
        try {
            Integer idCategorie = request.getInt("idCategorie");
            if (idCategorie == null) {
                return AppResponse.badRequest("L'identifiant de la catégorie est requis");
            }
            logger.info("Action: produits par catégorie id={}", idCategorie);
            List<Produit> produits = produitService.getProduitsByCategorie(idCategorie);
            return AppResponse.success(produits);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche par catégorie", e);
            return AppResponse.error("Erreur lors de la recherche par catégorie");
        }
    }

    //  RECHERCHE PAR NOM
    public String rechercher(AppRequest request) {
        try {
            String nom = request.getString("nom");
            if (nom == null || nom.isEmpty()) {
                return AppResponse.badRequest("Le nom de recherche est requis");
            }
            logger.info("Action: recherche produits nom={}", nom);
            List<Produit> produits = produitService.rechercherParNom(nom);
            return AppResponse.success(produits);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche par nom", e);
            return AppResponse.error("Erreur lors de la recherche par nom");
        }
    }

    //  AJOUTER UN PRODUIT (ADMIN)
    public String ajouter(AppRequest request) {
        try {
            Produit produit = request.getPayloadAs(Produit.class);
            if (produit == null) {
                return AppResponse.badRequest("Les données du produit sont requises");
            }
            logger.info("Action: ajouter produit {}", produit.getNom());
            produitService.ajouterProduit(produit);
            return AppResponse.success(null, "Produit ajouté avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout du produit", e);
            return AppResponse.error("Erreur lors de l'ajout du produit");
        }
    }

    //  MODIFIER UN PRODUIT (ADMIN)
    public String modifier(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            Produit produit = request.getPayloadAs(Produit.class);
            if (id == null || produit == null) {
                return AppResponse.badRequest("L'identifiant et les données du produit sont requis");
            }
            logger.info("Action: modifier produit id={}", id);
            produitService.modifierProduit(id, produit);
            return AppResponse.success(null, "Produit modifié avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la modification du produit", e);
            return AppResponse.error("Erreur lors de la modification du produit");
        }
    }

    //  SUPPRIMER UN PRODUIT (ADMIN)
    public String supprimer(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            if (id == null) {
                return AppResponse.badRequest("L'identifiant du produit est requis");
            }
            logger.info("Action: supprimer produit id={}", id);
            produitService.supprimerProduit(id);
            return AppResponse.success(null, "Produit supprimé avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du produit", e);
            return AppResponse.error("Erreur lors de la suppression du produit");
        }
    }

    //  MISE A JOUR DU STOCK (ADMIN)
    public String updateStock(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            Integer nouveauStock = request.getInt("stock");
            if (id == null || nouveauStock == null) {
                return AppResponse.badRequest("L'identifiant et le stock sont requis");
            }
            logger.info("Action: update stock produit id={} stock={}", id, nouveauStock);
            boolean succes = produitService.updateStock(id, nouveauStock);
            if (!succes) {
                return AppResponse.error("Mise à jour du stock échouée");
            }
            return AppResponse.success(null, "Stock mis à jour avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du stock", e);
            return AppResponse.error("Erreur lors de la mise à jour du stock");
        }
    }
}