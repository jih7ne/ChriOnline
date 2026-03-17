package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Categorie;
import com.chrionline.chrionline.server.services.CategorieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CategorieController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(CategorieController.class);
    private final CategorieService categorieService;

    public CategorieController() {
        this.categorieService = AppConfig.getService(CategorieService.class);
    }

    // LISTER TOUTES LES CATEGORIES
    public String lister(AppRequest request) {
        try {
            logger.info("Action: lister toutes les catégories");
            List<Categorie> categories = categorieService.listerCategories();
            return AppResponse.success(categories, "Catégories récupérées avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la liste des catégories", e);
            return AppResponse.error("Erreur lors de la récupération des catégories");
        }
    }

    // DETAILS D'UNE CATEGORIE
    public String details(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            if (id == null) {
                return AppResponse.badRequest("L'identifiant de la catégorie est requis");
            }
            logger.info("Action: détails catégorie id={}", id);
            Categorie categorie = categorieService.getCategorieById(id);
            if (categorie == null) {
                return AppResponse.notFound("Catégorie");
            }
            return AppResponse.success(categorie);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la catégorie", e);
            return AppResponse.error("Erreur lors de la récupération de la catégorie");
        }
    }

    // AJOUTER UNE CATEGORIE (ADMIN)
    public String ajouter(AppRequest request) {
        try {
            Categorie categorie = request.getPayloadAs(Categorie.class);
            if (categorie == null) {
                return AppResponse.badRequest("Les données de la catégorie sont requises");
            }
            logger.info("Action: ajouter catégorie {}", categorie.getNom());
            categorieService.ajouterCategorie(categorie);
            return AppResponse.success(null, "Catégorie ajoutée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout de la catégorie", e);
            return AppResponse.error("Erreur lors de l'ajout de la catégorie");
        }
    }

    // MODIFIER UNE CATEGORIE (ADMIN)
    public String modifier(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            Categorie categorie = request.getPayloadAs(Categorie.class);
            if (id == null || categorie == null) {
                return AppResponse.badRequest("L'identifiant et les données de la catégorie sont requis");
            }
            logger.info("Action: modifier catégorie id={}", id);
            categorieService.modifierCategorie(id, categorie);
            return AppResponse.success(null, "Catégorie modifiée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la modification de la catégorie", e);
            return AppResponse.error("Erreur lors de la modification de la catégorie");
        }
    }

    // SUPPRIMER UNE CATEGORIE (ADMIN)
    public String supprimer(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            if (id == null) {
                return AppResponse.badRequest("L'identifiant de la catégorie est requis");
            }
            logger.info("Action: supprimer catégorie id={}", id);
            categorieService.supprimerCategorie(id);
            return AppResponse.success(null, "Catégorie supprimée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la catégorie", e);
            return AppResponse.error("Erreur lors de la suppression de la catégorie");
        }
    }
}