package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.server.data.models.Produit;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProduitService {
    private static final Logger logger = LoggerFactory.getLogger(ProduitService.class);

    private final ProduitRepository produitRepository;

    public ProduitService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
        logger.info("ProduitService initialized");
    }

    // LISTER TOUS LES PRODUITS
    public List<Produit> listerProduits() {
        logger.info("Récupération de tous les produits");
        return produitRepository.findAll();
    }

    // DETAILS D'UN PRODUIT
    public Produit getProduitById(int id) {
        logger.info("Récupération du produit id={}", id);
        Produit produit = produitRepository.findById(id);
        if (produit == null) {
            logger.warn("Produit id={} non trouvé", id);
        }
        return produit;
    }

    //  RECHERCHE PAR CATEGORIE
    public List<Produit> getProduitsByCategorie(int idCategorie) {
        logger.info("Récupération des produits de la catégorie id={}", idCategorie);
        return produitRepository.findByCategorie(idCategorie);
    }

    //  RECHERCHE PAR NOM
    public List<Produit> rechercherParNom(String nom) {
        logger.info("Recherche des produits avec nom={}", nom);
        return produitRepository.findByNom(nom);
    }

    // AJOUTER UN PRODUIT
    public void ajouterProduit(Produit produit) {
        logger.info("Ajout du produit: {}", produit.getNom());
        produitRepository.add(produit);
    }

    //  MODIFIER UN PRODUIT
    public void modifierProduit(int id, Produit produit) {
        logger.info("Modification du produit id={}", id);
        Produit existant = produitRepository.findById(id);
        if (existant == null) {
            logger.warn("Produit id={} non trouvé pour modification", id);
            return;
        }
        produitRepository.update(String.valueOf(id), produit);
    }

    //  SUPPRIMER UN PRODUIT
    public void supprimerProduit(int id) {
        logger.info("Suppression du produit id={}", id);
        Produit existant = produitRepository.findById(id);
        if (existant == null) {
            logger.warn("Produit id={} non trouvé pour suppression", id);
            return;
        }
        produitRepository.deleteProduit(id);
    }

    //  MISE A JOUR DU STOCK
    public boolean updateStock(int id, int nouveauStock) {
        logger.info("Mise à jour stock produit id={} → stock={}", id, nouveauStock);
        if (nouveauStock < 0) {
            logger.warn("Stock négatif refusé pour produit id={}", id);
            return false;
        }
        Produit existant = produitRepository.findById(id);
        if (existant == null) {
            logger.warn("Produit id={} non trouvé pour mise à jour stock", id);
            return false;
        }
        produitRepository.updateStock(id, nouveauStock);
        return true;
    }

    //  VERIFIER DISPONIBILITE
    public boolean estDisponible(int id, int quantiteDemandee) {
        Produit produit = produitRepository.findById(id);
        if (produit == null) return false;
        return produit.getStock() >= quantiteDemandee;
    }
}