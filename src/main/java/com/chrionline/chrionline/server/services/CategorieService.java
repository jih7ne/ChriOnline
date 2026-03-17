package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.server.data.models.Categorie;
import com.chrionline.chrionline.server.repositories.CategorieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CategorieService {
    private static final Logger logger = LoggerFactory.getLogger(CategorieService.class);

    private final CategorieRepository categorieRepository;

    public CategorieService(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
        logger.info("CategorieService initialized");
    }

    public List<Categorie> listerCategories() {
        logger.info("Récupération de toutes les catégories");
        return categorieRepository.findAll();
    }

    public Categorie getCategorieById(int id) {
        logger.info("Récupération de la catégorie id={}", id);
        Categorie categorie = categorieRepository.findById(id);
        if (categorie == null) {
            logger.warn("Catégorie id={} non trouvée", id);
        }
        return categorie;
    }

    public void ajouterCategorie(Categorie categorie) {
        logger.info("Ajout de la catégorie: {}", categorie.getNom());
        categorieRepository.add(categorie);
    }

    public void modifierCategorie(int id, Categorie categorie) {
        logger.info("Modification de la catégorie id={}", id);
        Categorie existante = categorieRepository.findById(id);
        if (existante == null) {
            logger.warn("Catégorie id={} non trouvée pour modification", id);
            return;
        }
        categorieRepository.update(String.valueOf(id), categorie);
    }

    public void supprimerCategorie(int id) {
        logger.info("Suppression de la catégorie id={}", id);
        Categorie existante = categorieRepository.findById(id);
        if (existante == null) {
            logger.warn("Catégorie id={} non trouvée pour suppression", id);
            return;
        }
        categorieRepository.deleteCategorie(id);
    }
}